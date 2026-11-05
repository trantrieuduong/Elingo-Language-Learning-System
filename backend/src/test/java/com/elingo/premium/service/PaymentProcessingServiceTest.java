package com.elingo.premium.service;

import com.elingo.common.event.PaymentSucceededEvent;
import com.elingo.premium.dto.PaymentProcessingResult;
import com.elingo.premium.entity.PaymentProvider;
import com.elingo.premium.entity.PaymentTransaction;
import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.PremiumPlan;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.entity.WebhookLogStatus;
import com.elingo.premium.gateway.WebhookEvent;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.service.impl.PaymentProcessingServiceImpl;
import com.elingo.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PaymentProcessingServiceTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentProcessingServiceImpl paymentProcessingService;

    private User testUser;
    private PremiumPlan testPlan;
    private PaymentProvider testProvider;
    private PaymentTransaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("user1").build();
        testPlan = PremiumPlan.builder().id(10L).name("Gói 1 tháng").build();
        testProvider = PaymentProvider.builder().id(2L).code("SEPAY").build();

        testTransaction = PaymentTransaction.builder()
                .id(100L)
                .user(testUser)
                .plan(testPlan)
                .providerRefCode("ELGO_REF_123")
                .amount(new BigDecimal("99000"))
                .currency("VND")
                .status(PaymentTransactionStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("process tests")
    class ProcessTests {

        @Test
        @DisplayName("Should return IGNORED when event is null")
        void process_ReturnsIgnored_WhenEventIsNull() {
            PaymentProcessingResult result = paymentProcessingService.process(null, testProvider);

            assertThat(result.status()).isEqualTo(WebhookLogStatus.IGNORED);
            assertThat(result.message()).contains("Event is null");
        }

        @Test
        @DisplayName("Should return IGNORED when event is not successful or non-inflow")
        void process_ReturnsIgnored_WhenEventNotSuccess() {
            WebhookEvent event = new WebhookEvent("evt_1", "ELGO_REF_123", new BigDecimal("99000"), false, "{}");

            PaymentProcessingResult result = paymentProcessingService.process(event, testProvider);

            assertThat(result.status()).isEqualTo(WebhookLogStatus.IGNORED);
            assertThat(result.message()).contains("failure or non-inflow");
        }

        @Test
        @DisplayName("Should return IGNORED when refCode is blank")
        void process_ReturnsIgnored_WhenRefCodeIsBlank() {
            WebhookEvent event = new WebhookEvent("evt_1", "  ", new BigDecimal("99000"), true, "{}");

            PaymentProcessingResult result = paymentProcessingService.process(event, testProvider);

            assertThat(result.status()).isEqualTo(WebhookLogStatus.IGNORED);
            assertThat(result.message()).contains("Reference code is missing");
        }

        @Test
        @DisplayName("Should return IGNORED when transaction is not found in database")
        void process_ReturnsIgnored_WhenTransactionNotFound() {
            WebhookEvent event = new WebhookEvent("evt_1", "ELGO_NOT_EXIST", new BigDecimal("99000"), true, "{}");
            when(transactionRepository.findByRefCodeForUpdate("ELGO_NOT_EXIST")).thenReturn(Optional.empty());

            PaymentProcessingResult result = paymentProcessingService.process(event, testProvider);

            assertThat(result.status()).isEqualTo(WebhookLogStatus.IGNORED);
            assertThat(result.message()).contains("Transaction not found");
        }

        @Test
        @DisplayName("Should return IGNORED when transaction is already resolved")
        void process_ReturnsIgnored_WhenTransactionAlreadyResolved() {
            testTransaction.setStatus(PaymentTransactionStatus.SUCCESS);
            WebhookEvent event = new WebhookEvent("evt_1", "ELGO_REF_123", new BigDecimal("99000"), true, "{}");
            when(transactionRepository.findByRefCodeForUpdate("ELGO_REF_123")).thenReturn(Optional.of(testTransaction));

            PaymentProcessingResult result = paymentProcessingService.process(event, testProvider);

            assertThat(result.status()).isEqualTo(WebhookLogStatus.IGNORED);
            assertThat(result.message()).contains("Transaction already resolved");
            verify(subscriptionService, never()).activateOrExtend(any(), any());
        }

        @Test
        @DisplayName("Should mark metadata as underpaid and return IGNORED when payment amount is less than expected")
        void process_MarksUnderpaid_WhenAmountIsLessThanExpected() {
            WebhookEvent event = new WebhookEvent("evt_1", "ELGO_REF_123", new BigDecimal("50000"), true, "{}");
            when(transactionRepository.findByRefCodeForUpdate("ELGO_REF_123")).thenReturn(Optional.of(testTransaction));

            PaymentProcessingResult result = paymentProcessingService.process(event, testProvider);

            assertThat(result.status()).isEqualTo(WebhookLogStatus.IGNORED);
            assertThat(result.message()).contains("Underpaid");
            assertThat(testTransaction.getMetadata()).contains("underpaid");
            assertThat(testTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING); // Vẫn giữ PENDING để đối soát tay
            verify(transactionRepository).save(testTransaction);
            verify(subscriptionService, never()).activateOrExtend(any(), any());
        }

        @Test
        @DisplayName("Should process successfully: update status to SUCCESS, assign provider, activate subscription and publish event")
        void process_Success_WhenAmountMatches() {
            WebhookEvent event = new WebhookEvent("evt_1", "ELGO_REF_123", new BigDecimal("99000"), true, "{}");
            when(transactionRepository.findByRefCodeForUpdate("ELGO_REF_123")).thenReturn(Optional.of(testTransaction));

            UserSubscription createdSub = UserSubscription.builder().id(200L).build();
            when(subscriptionService.activateOrExtend(1L, 10L)).thenReturn(createdSub);

            PaymentProcessingResult result = paymentProcessingService.process(event, testProvider);

            assertThat(result.status()).isEqualTo(WebhookLogStatus.PROCESSED);
            assertThat(result.transactionId()).isEqualTo(100L);
            assertThat(testTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCESS);
            assertThat(testTransaction.getProvider()).isEqualTo(testProvider);
            assertThat(testTransaction.getPaidAt()).isNotNull();
            assertThat(testTransaction.getSubscription()).isEqualTo(createdSub);

            verify(transactionRepository).save(testTransaction);
            verify(subscriptionService).activateOrExtend(1L, 10L);

            ArgumentCaptor<PaymentSucceededEvent> eventCaptor = ArgumentCaptor.forClass(PaymentSucceededEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().transactionId()).isEqualTo(100L);
            assertThat(eventCaptor.getValue().userId()).isEqualTo(1L);
            assertThat(eventCaptor.getValue().planId()).isEqualTo(10L);
        }
    }
}
