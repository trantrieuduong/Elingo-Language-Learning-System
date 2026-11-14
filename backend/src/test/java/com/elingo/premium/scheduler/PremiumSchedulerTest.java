package com.elingo.premium.scheduler;

import com.elingo.common.event.NotificationEvent;
import com.elingo.notification.entity.NotificationType;
import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.PremiumPlan;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PremiumSchedulerTest {

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PremiumScheduler premiumScheduler;

    @Nested
    @DisplayName("scanAndExpireSubscriptions")
    class ScanAndExpireSubscriptionsTests {

        @Test
        @DisplayName("Should update status to EXPIRED for all expired subscriptions successfully")
        void scanAndExpireSubscriptions_Success() {
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
        @DisplayName("Should execute normally when no subscriptions expired")
        void scanAndExpireSubscriptions_WhenNoneExpired() {
            when(userSubscriptionRepository.updateStatusForExpiredSubscriptions(
                    eq(SubscriptionStatus.EXPIRED),
                    eq(SubscriptionStatus.ACTIVE),
                    any(LocalDateTime.class)
            )).thenReturn(0);

            premiumScheduler.scanAndExpireSubscriptions();

            verify(userSubscriptionRepository).updateStatusForExpiredSubscriptions(
                    eq(SubscriptionStatus.EXPIRED),
                    eq(SubscriptionStatus.ACTIVE),
                    any(LocalDateTime.class)
            );
        }
    }

    @Nested
    @DisplayName("cancelStalePendingTransactions")
    class CancelStalePendingTransactionsTests {

        @Test
        @DisplayName("Should update status to FAILED for pending transactions older than 24 hours")
        void cancelStalePendingTransactions_Success() {
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

        @Test
        @DisplayName("Should execute normally when no stale transactions found")
        void cancelStalePendingTransactions_WhenNoneStale() {
            when(paymentTransactionRepository.cancelStaleTransactions(
                    eq(PaymentTransactionStatus.FAILED),
                    eq(PaymentTransactionStatus.PENDING),
                    any(LocalDateTime.class)
            )).thenReturn(0);

            premiumScheduler.cancelStalePendingTransactions();

            verify(paymentTransactionRepository).cancelStaleTransactions(
                    eq(PaymentTransactionStatus.FAILED),
                    eq(PaymentTransactionStatus.PENDING),
                    any(LocalDateTime.class)
            );
        }
    }

    @Nested
    @DisplayName("scanAndNotifyExpiringSubscriptions")
    class ScanAndNotifyExpiringSubscriptionsTests {

        @Test
        @DisplayName("Should publish NotificationEvent for each expiring subscription")
        void scanAndNotifyExpiringSubscriptions_Success_PublishesEvents() {
            User user1 = User.builder().id(10L).username("user1").build();
            PremiumPlan plan1 = PremiumPlan.builder().id(100L).name("Gói 1 tháng").build();
            UserSubscription sub1 = UserSubscription.builder()
                    .id(1L)
                    .user(user1)
                    .plan(plan1)
                    .status(SubscriptionStatus.ACTIVE)
                    .endAt(LocalDateTime.of(2026, 11, 16, 8, 0))
                    .build();

            User user2 = User.builder().id(20L).username("user2").build();
            PremiumPlan plan2 = PremiumPlan.builder().id(200L).name("Gói 1 năm").build();
            UserSubscription sub2 = UserSubscription.builder()
                    .id(2L)
                    .user(user2)
                    .plan(plan2)
                    .status(SubscriptionStatus.ACTIVE)
                    .endAt(LocalDateTime.of(2026, 11, 17, 12, 0))
                    .build();

            when(userSubscriptionRepository.findExpiringSubscriptions(
                    eq(SubscriptionStatus.ACTIVE),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            )).thenReturn(List.of(sub1, sub2));

            premiumScheduler.scanAndNotifyExpiringSubscriptions();

            ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

            List<NotificationEvent> capturedEvents = eventCaptor.getAllValues();
            assertThat(capturedEvents).hasSize(2);

            NotificationEvent event1 = capturedEvents.get(0);
            assertThat(event1.recipientId()).isEqualTo(10L);
            assertThat(event1.type()).isEqualTo(NotificationType.SUBSCRIPTION_EXPIRING);
            assertThat(event1.refId()).isEqualTo(1L);
            assertThat(event1.args()).containsEntry("planName", "Gói 1 tháng");
            assertThat(event1.args()).containsEntry("expiresAt", "16/11/2026");

            NotificationEvent event2 = capturedEvents.get(1);
            assertThat(event2.recipientId()).isEqualTo(20L);
            assertThat(event2.type()).isEqualTo(NotificationType.SUBSCRIPTION_EXPIRING);
            assertThat(event2.refId()).isEqualTo(2L);
            assertThat(event2.args()).containsEntry("planName", "Gói 1 năm");
            assertThat(event2.args()).containsEntry("expiresAt", "17/11/2026");
        }

        @Test
        @DisplayName("Should not publish any event when no expiring subscriptions exist")
        void scanAndNotifyExpiringSubscriptions_WhenEmpty_DoesNotPublishEvent() {
            when(userSubscriptionRepository.findExpiringSubscriptions(
                    eq(SubscriptionStatus.ACTIVE),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            )).thenReturn(List.of());

            premiumScheduler.scanAndNotifyExpiringSubscriptions();

            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}
