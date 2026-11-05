package com.elingo.premium.scheduler;

import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PremiumSchedulerTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @InjectMocks
    private PremiumScheduler premiumScheduler;

    @Test
    @DisplayName("scanAndExpireSubscriptions should update status to EXPIRED for all expired subscriptions")
    void scanAndExpireSubscriptions_CallsRepository() {
        when(userSubscriptionRepository.updateStatusForExpiredSubscriptions(
                eq(SubscriptionStatus.EXPIRED),
                eq(SubscriptionStatus.ACTIVE),
                any(LocalDateTime.class)
        )).thenReturn(5);

        premiumScheduler.scanAndExpireSubscriptions();

        verify(userSubscriptionRepository).updateStatusForExpiredSubscriptions(
                eq(SubscriptionStatus.EXPIRED),
                eq(SubscriptionStatus.ACTIVE),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("cancelStalePendingTransactions should update status to FAILED for pending transactions older than 24 hours")
    void cancelStalePendingTransactions_CallsRepository() {
        when(paymentTransactionRepository.cancelStaleTransactions(
                eq(PaymentTransactionStatus.FAILED),
                eq(PaymentTransactionStatus.PENDING),
                any(LocalDateTime.class)
        )).thenReturn(3);

        premiumScheduler.cancelStalePendingTransactions();

        verify(paymentTransactionRepository).cancelStaleTransactions(
                eq(PaymentTransactionStatus.FAILED),
                eq(PaymentTransactionStatus.PENDING),
                any(LocalDateTime.class)
        );
    }
}
