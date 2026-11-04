package com.elingo.premium.service.impl;

import com.elingo.premium.dto.PaymentProcessingResult;
import com.elingo.premium.entity.PaymentProvider;
import com.elingo.premium.entity.PaymentTransaction;
import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.entity.WebhookLogStatus;
import com.elingo.common.event.PaymentSucceededEvent;
import com.elingo.premium.gateway.WebhookEvent;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.service.PaymentProcessingService;
import com.elingo.premium.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-PROCESSING-SERVICE")
public class PaymentProcessingServiceImpl implements PaymentProcessingService {

    private final PaymentTransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public PaymentProcessingResult process(WebhookEvent event, PaymentProvider provider) {
        if (event == null) {
            return new PaymentProcessingResult(null, WebhookLogStatus.IGNORED, "Event is null");
        }

        if (!event.success()) {
            log.info("Event marked as non-success or non-inflow eventId={} refCode={}",
                    event.eventId(), event.refCode());
            return new PaymentProcessingResult(null, WebhookLogStatus.IGNORED, "Event indicates failure or non-inflow");
        }

        if (event.refCode() == null || event.refCode().isBlank()) {
            log.warn("Event missing reference code eventId={}", event.eventId());
            return new PaymentProcessingResult(null, WebhookLogStatus.IGNORED, "Reference code is missing");
        }

        // a. findByRefCodeForUpdate (PESSIMISTIC_WRITE); không thấy thì IGNORED
        Optional<PaymentTransaction> txOpt = transactionRepository.findByRefCodeForUpdate(event.refCode());
        if (txOpt.isEmpty()) {
            log.warn("Payment transaction not found for refCode={}", event.refCode());
            return new PaymentProcessingResult(null, WebhookLogStatus.IGNORED, "Transaction not found");
        }

        PaymentTransaction tx = txOpt.get();

        // b. status khác PENDING thì IGNORED
        if (tx.getStatus() != PaymentTransactionStatus.PENDING) {
            log.info("Payment transaction already resolved txId={} status={}", tx.getId(), tx.getStatus());
            return new PaymentProcessingResult(tx.getId(), WebhookLogStatus.IGNORED, "Transaction already resolved");
        }

        // c. amount nhận được nhỏ hơn amount giao dịch thì KHÔNG cộng premium, đánh dấu để đối soát tay
        if (event.amount() == null || event.amount().compareTo(tx.getAmount()) < 0) {
            log.warn("Underpaid amount for txId={} refCode={} expected={} actual={}",
                    tx.getId(), tx.getProviderRefCode(), tx.getAmount(), event.amount());
            String metadataNote = String.format("{\"underpaid\":true,\"receivedAmount\":%s,\"note\":\"Chờ đối soát thủ công\"}",
                    event.amount() != null ? event.amount().toPlainString() : "0");
            tx.setMetadata(metadataNote);
            if (provider != null) {
                tx.setProvider(provider);
            }
            transactionRepository.save(tx);
            return new PaymentProcessingResult(tx.getId(), WebhookLogStatus.IGNORED, "Underpaid: received amount is less than expected");
        }

        // d. set SUCCESS + paid_at = now + provider (nếu có từ webhook)
        LocalDateTime now = LocalDateTime.now();
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setPaidAt(now);
        if (provider != null) {
            tx.setProvider(provider);
        }

        // e. subscriptionService.activateOrExtend(userId, planId)
        UserSubscription subscription = subscriptionService.activateOrExtend(
                tx.getUser().getId(),
                tx.getPlan().getId()
        );

        // f. gán tx.subscription, publish PaymentSucceededEvent
        tx.setSubscription(subscription);
        transactionRepository.save(tx);

        log.info("Payment transaction completed successfully txId={} userId={} planId={} subId={} provider={}",
                tx.getId(), tx.getUser().getId(), tx.getPlan().getId(), subscription.getId(),
                provider != null ? provider.getCode() : "UNKNOWN");

        eventPublisher.publishEvent(new PaymentSucceededEvent(
                tx.getId(),
                tx.getUser().getId(),
                tx.getPlan().getId(),
                tx.getAmount(),
                tx.getPaidAt()
        ));

        return new PaymentProcessingResult(tx.getId(), WebhookLogStatus.PROCESSED, "Payment processed successfully");
    }
}
