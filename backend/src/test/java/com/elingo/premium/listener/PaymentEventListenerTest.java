package com.elingo.premium.listener;

import com.elingo.common.event.PaymentSucceededEvent;
import com.elingo.common.service.EmailService;
import com.elingo.premium.entity.PaymentTransaction;
import com.elingo.premium.entity.PremiumPlan;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.repository.PremiumPlanRepository;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PaymentEventListenerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PremiumPlanRepository premiumPlanRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    private User testUser;
    private PremiumPlan testPlan;
    private PaymentTransaction testTx;
    private UserSubscription testSub;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("testuser").email("user@elingo.com").build();
        testPlan = PremiumPlan.builder().id(10L).name("Gói 1 tháng").build();
        testSub = UserSubscription.builder().id(50L).endAt(LocalDateTime.now().plusDays(30)).build();
        testTx = PaymentTransaction.builder()
                .id(100L)
                .providerRefCode("ELGO_REF_100")
                .currency("VND")
                .subscription(testSub)
                .build();
    }

    @Test
    @DisplayName("Should send confirmation email when user exists and has email address")
    void onPaymentSucceeded_SendsEmail_WhenUserHasEmail() {
        LocalDateTime paidAt = LocalDateTime.now();
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                100L, 1L, 10L, new BigDecimal("99000"), paidAt
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(premiumPlanRepository.findById(10L)).thenReturn(Optional.of(testPlan));
        when(paymentTransactionRepository.findById(100L)).thenReturn(Optional.of(testTx));

        paymentEventListener.onPaymentSucceeded(event);

        verify(emailService).sendPaymentSuccessEmail(
                eq("user@elingo.com"),
                eq("testuser"),
                eq("Gói 1 tháng"),
                eq(new BigDecimal("99000")),
                eq("VND"),
                eq("ELGO_REF_100"),
                eq(paidAt),
                eq(testSub.getEndAt())
        );
    }

    @Test
    @DisplayName("Should not send email when user does not exist or has empty email")
    void onPaymentSucceeded_DoesNotSendEmail_WhenUserHasNoEmail() {
        testUser.setEmail("");
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                100L, 1L, 10L, new BigDecimal("99000"), LocalDateTime.now()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        paymentEventListener.onPaymentSucceeded(event);

        verify(emailService, never()).sendPaymentSuccessEmail(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
