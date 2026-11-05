package com.elingo.premium.scheduler;

import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PREMIUM-SCHEDULER")
public class PremiumScheduler {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

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
            log.info("Expired {} subscriptions at {}", count, now);
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
            log.info("Cancelled {} stale pending transactions created before {}", count, threshold);
        }
    }
}

