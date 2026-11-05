package com.elingo.premium.listener;

import com.elingo.common.service.EmailService;
import com.elingo.premium.entity.PaymentTransaction;
import com.elingo.premium.entity.PremiumPlan;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.common.event.PaymentSucceededEvent;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.repository.PremiumPlanRepository;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-EVENT-LISTENER")
public class PaymentEventListener {

    private final UserRepository userRepository;
    private final PremiumPlanRepository premiumPlanRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Payment succeeded event received txId={} userId={} planId={} amount={}",
                event.transactionId(), event.userId(), event.planId(), event.amount());

        try {
            User user = userRepository.findById(event.userId()).orElse(null);
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                log.warn("Cannot send payment success email: User not found or has no email userId={}", event.userId());
                return;
            }

            PremiumPlan plan = premiumPlanRepository.findById(event.planId()).orElse(null);
            String planName = (plan != null) ? plan.getName() : "Premium";

            PaymentTransaction tx = paymentTransactionRepository.findById(event.transactionId()).orElse(null);
            String refCode = (tx != null) ? tx.getProviderRefCode() : "";
            String currency = (tx != null && tx.getCurrency() != null) ? tx.getCurrency() : "VND";

            LocalDateTime endAt = null;
            if (tx != null && tx.getSubscription() != null) {
                endAt = tx.getSubscription().getEndAt();
            } else {
                UserSubscription sub = userSubscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(event.userId()).orElse(null);
                if (sub != null) {
                    endAt = sub.getEndAt();
                }
            }

            emailService.sendPaymentSuccessEmail(
                    user.getEmail(),
                    user.getUsername(),
                    planName,
                    event.amount(),
                    currency,
                    refCode,
                    event.paidAt(),
                    endAt
            );
        } catch (Exception e) {
            log.error("Error processing PaymentSucceededEvent txId={}: {}", event.transactionId(), e.getMessage(), e);
        }
    }
}
