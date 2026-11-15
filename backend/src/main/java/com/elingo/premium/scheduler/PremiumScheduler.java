package com.elingo.premium.scheduler;

import com.elingo.common.event.NotificationEvent;
import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PREMIUM-SCHEDULER")
public class PremiumScheduler {

    private static final DateTimeFormatter EXPIRES_AT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Tự động quét và cập nhật trạng thái các subscription đã hết hạn sang EXPIRED.
     */
    @Scheduled(cron = "${app.premium.scheduler.expire-cron}")
    @Transactional
    public void scanAndExpireSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        int count = userSubscriptionRepository.updateStatusForExpiredSubscriptions(
                SubscriptionStatus.EXPIRED,
                SubscriptionStatus.ACTIVE,
                now
        );
        if (count > 0) {
            log.info("Subscriptions expired count={}", count);
        }
    }

    /**
     * Tự động hủy các giao dịch thanh toán PENDING quá 24 giờ mà không nhận được tiền.
     */
    @Scheduled(cron = "${app.premium.scheduler.cancel-tx-cron}")
    @Transactional
    public void cancelStalePendingTransactions() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        int count = paymentTransactionRepository.cancelStaleTransactions(
                PaymentTransactionStatus.FAILED,
                PaymentTransactionStatus.PENDING,
                threshold
        );
        if (count > 0) {
            log.info("Stale pending transactions cancelled count={}", count);
        }
    }

    /**
     * Tự động quét các gói subscription sắp hết hạn trong vòng 3 ngày để gửi thông báo.
     */
    @Scheduled(cron = "${app.premium.scheduler.expiring-cron:0 0 8 * * *}")
    @Transactional(readOnly = true)
    public void scanAndNotifyExpiringSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(3);
        List<UserSubscription> expiringSubs = userSubscriptionRepository.findExpiringSubscriptions(
                SubscriptionStatus.ACTIVE, now, threshold);

        int count = 0;
        for (UserSubscription sub : expiringSubs) {
            String expiresAt = sub.getEndAt() != null ? sub.getEndAt().format(EXPIRES_AT_FORMATTER) : "";
            eventPublisher.publishEvent(NotificationEvent.subscriptionExpiring(
                    sub.getUser().getId(),
                    sub.getId(),
                    sub.getPlan().getName(),
                    expiresAt
            ));
            count++;
        }
        if (count > 0) {
            log.info("Expiring subscription notifications sent count={}", count);
        }
    }
}

