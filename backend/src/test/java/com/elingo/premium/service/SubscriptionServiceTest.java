package com.elingo.premium.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.entity.PremiumPlan;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.repository.PremiumPlanRepository;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.premium.service.impl.SubscriptionServiceImpl;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SubscriptionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PremiumPlanRepository premiumPlanRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private User testUser;
    private PremiumPlan testPlan;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("test_user")
                .email("test@elingo.com")
                .build();

        testPlan = PremiumPlan.builder()
                .id(10L)
                .name("Gói 1 tháng")
                .price(new BigDecimal("99000"))
                .durationDays(30)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("activateOrExtend tests")
    class ActivateOrExtendTests {

        @Test
        @DisplayName("Should create new subscription when user has no active subscription")
        void activateOrExtend_CreatesNewSubscription_WhenNoActiveSub() {
            when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testUser));
            when(premiumPlanRepository.findById(10L)).thenReturn(Optional.of(testPlan));
            when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndAtDesc(1L, SubscriptionStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            when(userSubscriptionRepository.save(any(UserSubscription.class)))
                    .thenAnswer(invocation -> {
                        UserSubscription sub = invocation.getArgument(0);
                        sub.setId(100L);
                        return sub;
                    });

            UserSubscription result = subscriptionService.activateOrExtend(1L, 10L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getPlan()).isEqualTo(testPlan);
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getStartAt()).isNotNull();
            assertThat(result.getEndAt()).isAfter(result.getStartAt());
        }

        @Test
        @DisplayName("Should extend end date when user has an unexpired active subscription")
        void activateOrExtend_ExtendsEndDate_WhenActiveSubNotExpired() {
            LocalDateTime existingEndAt = LocalDateTime.now().plusDays(10);
            UserSubscription existingSub = UserSubscription.builder()
                    .id(50L)
                    .user(testUser)
                    .plan(testPlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startAt(LocalDateTime.now().minusDays(20))
                    .endAt(existingEndAt)
                    .build();

            when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testUser));
            when(premiumPlanRepository.findById(10L)).thenReturn(Optional.of(testPlan));
            when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndAtDesc(1L, SubscriptionStatus.ACTIVE))
                    .thenReturn(Optional.of(existingSub));
            when(userSubscriptionRepository.save(existingSub)).thenReturn(existingSub);

            UserSubscription result = subscriptionService.activateOrExtend(1L, 10L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(50L);
            assertThat(result.getEndAt()).isEqualTo(existingEndAt.plusDays(30));
        }

        @Test
        @DisplayName("Should mark old subscription as EXPIRED and create new one when active subscription is past end date")
        void activateOrExtend_ExpiresOldAndCreatesNew_WhenActiveSubAlreadyPassed() {
            LocalDateTime oldEndAt = LocalDateTime.now().minusDays(2);
            UserSubscription expiredSub = UserSubscription.builder()
                    .id(60L)
                    .user(testUser)
                    .plan(testPlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startAt(LocalDateTime.now().minusDays(32))
                    .endAt(oldEndAt)
                    .build();

            when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testUser));
            when(premiumPlanRepository.findById(10L)).thenReturn(Optional.of(testPlan));
            when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndAtDesc(1L, SubscriptionStatus.ACTIVE))
                    .thenReturn(Optional.of(expiredSub));

            when(userSubscriptionRepository.save(any(UserSubscription.class)))
                    .thenAnswer(invocation -> {
                        UserSubscription sub = invocation.getArgument(0);
                        if (sub.getId() == null) {
                            sub.setId(101L);
                        }
                        return sub;
                    });

            UserSubscription result = subscriptionService.activateOrExtend(1L, 10L);

            assertThat(expiredSub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
            assertThat(result.getId()).isEqualTo(101L);
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when userId does not exist")
        void activateOrExtend_ThrowsException_WhenUserNotFound() {
            when(userRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.activateOrExtend(999L, 10L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw PREMIUM_PLAN_NOT_FOUND when planId does not exist")
        void activateOrExtend_ThrowsException_WhenPlanNotFound() {
            when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testUser));
            when(premiumPlanRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.activateOrExtend(1L, 999L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PREMIUM_PLAN_NOT_FOUND);
        }
    }
}
