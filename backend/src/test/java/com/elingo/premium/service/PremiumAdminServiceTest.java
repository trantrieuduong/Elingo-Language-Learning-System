package com.elingo.premium.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.event.NotificationEvent;
import com.elingo.common.event.PaymentSucceededEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.dto.request.AdminTransactionFilterRequest;
import com.elingo.premium.dto.request.AdminWebhookLogFilterRequest;
import com.elingo.premium.dto.request.CreatePremiumPlanRequest;
import com.elingo.premium.dto.request.TransactionReasonRequest;
import com.elingo.premium.dto.request.UpdatePremiumPlanRequest;
import com.elingo.premium.dto.response.AdminPremiumPlanResponse;
import com.elingo.premium.dto.response.AdminTransactionDetailResponse;
import com.elingo.premium.dto.response.AdminTransactionResponse;
import com.elingo.premium.dto.response.AdminWebhookLogDetailResponse;
import com.elingo.premium.dto.response.AdminWebhookLogResponse;
import com.elingo.premium.entity.PaymentProvider;
import com.elingo.premium.entity.PaymentTransaction;
import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.PaymentWebhookLog;
import com.elingo.premium.entity.PremiumPlan;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.entity.WebhookLogStatus;
import com.elingo.premium.mapper.PremiumMapper;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.repository.PaymentWebhookLogRepository;
import com.elingo.premium.repository.PremiumPlanRepository;
import com.elingo.premium.service.impl.PremiumAdminServiceImpl;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("PremiumAdminService")
class PremiumAdminServiceTest {

    @Mock private PremiumPlanRepository premiumPlanRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private PaymentWebhookLogRepository paymentWebhookLogRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private PremiumMapper premiumMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PremiumAdminServiceImpl premiumAdminService;

    private User testUser;
    private PremiumPlan activePlan;
    private PremiumPlan inactivePlan;
    private AdminPremiumPlanResponse activePlanResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(10L)
                .username("john_doe")
                .email("john@elingo.com")
                .fullName("John Doe")
                .build();

        activePlan = PremiumPlan.builder()
                .id(1L)
                .name("Pro Monthly")
                .price(new BigDecimal("99000.00"))
                .currency("VND")
                .durationDays(30)
                .isActive(true)
                .build();

        inactivePlan = PremiumPlan.builder()
                .id(2L)
                .name("Pro Quarterly")
                .price(new BigDecimal("249000.00"))
                .currency("VND")
                .durationDays(90)
                .isActive(false)
                .build();

        activePlanResponse = new AdminPremiumPlanResponse(
                1L, "Pro Monthly", new BigDecimal("99000.00"), "VND", 30, true,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("getAllPlansForAdmin")
    class GetAllPlansForAdmin {

        @Test
        @DisplayName("should return all plans including inactive ones")
        void shouldReturnAllPlans_whenCalled() {
            when(premiumPlanRepository.findAllByOrderByPriceAscIdAsc()).thenReturn(List.of(activePlan, inactivePlan));
            when(premiumMapper.toAdminPremiumPlanResponseList(List.of(activePlan, inactivePlan)))
                    .thenReturn(List.of(activePlanResponse));

            List<AdminPremiumPlanResponse> result = premiumAdminService.getAllPlansForAdmin();

            assertThat(result).hasSize(1);
            verify(premiumPlanRepository).findAllByOrderByPriceAscIdAsc();
        }
    }

    @Nested
    @DisplayName("getPlanByIdForAdmin")
    class GetPlanByIdForAdmin {

        @Test
        @DisplayName("should return plan when id exists")
        void shouldReturnPlan_whenIdExists() {
            when(premiumPlanRepository.findById(1L)).thenReturn(Optional.of(activePlan));
            when(premiumMapper.toAdminPremiumPlanResponse(activePlan)).thenReturn(activePlanResponse);

            AdminPremiumPlanResponse result = premiumAdminService.getPlanByIdForAdmin(1L);

            assertThat(result).isEqualTo(activePlanResponse);
        }

        @Test
        @DisplayName("should throw PREMIUM_PLAN_NOT_FOUND when id does not exist")
        void shouldThrowNotFound_whenIdDoesNotExist() {
            when(premiumPlanRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> premiumAdminService.getPlanByIdForAdmin(99L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PREMIUM_PLAN_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("createPlan")
    class CreatePlan {

        @Test
        @DisplayName("should normalize currency, default isActive to false, and save plan")
        void shouldCreateInactivePlanWithNormalizedCurrency() {
            CreatePremiumPlanRequest request = new CreatePremiumPlanRequest(
                    "Pro Quarterly", new BigDecimal("249000.00"), " vnd ", 90
            );
            PremiumPlan mapped = PremiumPlan.builder()
                    .name("Pro Quarterly")
                    .price(new BigDecimal("249000.00"))
                    .currency(" vnd ")
                    .durationDays(90)
                    .isActive(true)
                    .build();
            AdminPremiumPlanResponse expected = new AdminPremiumPlanResponse(
                    2L, "Pro Quarterly", new BigDecimal("249000.00"), "VND", 90, false,
                    LocalDateTime.now(), LocalDateTime.now()
            );

            when(premiumMapper.toEntity(request)).thenReturn(mapped);
            when(premiumPlanRepository.save(mapped)).thenReturn(mapped);
            when(premiumMapper.toAdminPremiumPlanResponse(mapped)).thenReturn(expected);

            AdminPremiumPlanResponse result = premiumAdminService.createPlan(request);

            assertThat(mapped.getCurrency()).isEqualTo("VND");
            assertThat(mapped.getIsActive()).isFalse();
            assertThat(result.isActive()).isFalse();
            verify(premiumPlanRepository).save(mapped);
        }
    }

    @Nested
    @DisplayName("updatePlan")
    class UpdatePlan {

        @Test
        @DisplayName("should update plan name and price when plan exists")
        void shouldUpdatePlan_whenExists() {
            UpdatePremiumPlanRequest request = new UpdatePremiumPlanRequest("Pro Updated", new BigDecimal("109000.00"));
            when(premiumPlanRepository.findById(1L)).thenReturn(Optional.of(activePlan));
            when(premiumPlanRepository.save(activePlan)).thenReturn(activePlan);
            when(premiumMapper.toAdminPremiumPlanResponse(activePlan)).thenReturn(activePlanResponse);

            AdminPremiumPlanResponse result = premiumAdminService.updatePlan(1L, request);

            assertThat(result).isEqualTo(activePlanResponse);
            verify(premiumMapper).updateEntity(activePlan, request);
            verify(premiumPlanRepository).save(activePlan);
        }

        @Test
        @DisplayName("should throw PREMIUM_PLAN_NOT_FOUND when plan does not exist")
        void shouldThrowNotFound_whenPlanDoesNotExist() {
            UpdatePremiumPlanRequest request = new UpdatePremiumPlanRequest("Pro", BigDecimal.TEN);
            when(premiumPlanRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> premiumAdminService.updatePlan(99L, request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PREMIUM_PLAN_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("disablePlan")
    class DisablePlan {

        @Test
        @DisplayName("should set isActive to false when plan is active")
        void shouldDisablePlan_whenActive() {
            when(premiumPlanRepository.findById(1L)).thenReturn(Optional.of(activePlan));
            when(premiumPlanRepository.save(activePlan)).thenReturn(activePlan);
            when(premiumMapper.toAdminPremiumPlanResponse(activePlan)).thenReturn(activePlanResponse);

            premiumAdminService.disablePlan(1L);

            assertThat(activePlan.getIsActive()).isFalse();
            verify(premiumPlanRepository).save(activePlan);
        }

        @Test
        @DisplayName("should throw PREMIUM_PLAN_ALREADY_INACTIVE when plan is already disabled")
        void shouldThrowAlreadyInactive_whenAlreadyDisabled() {
            when(premiumPlanRepository.findById(2L)).thenReturn(Optional.of(inactivePlan));

            assertThatThrownBy(() -> premiumAdminService.disablePlan(2L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PREMIUM_PLAN_ALREADY_INACTIVE);

            verify(premiumPlanRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("enablePlan")
    class EnablePlan {

        @Test
        @DisplayName("should set isActive to true when plan is inactive")
        void shouldEnablePlan_whenInactive() {
            when(premiumPlanRepository.findById(2L)).thenReturn(Optional.of(inactivePlan));
            when(premiumPlanRepository.save(inactivePlan)).thenReturn(inactivePlan);
            when(premiumMapper.toAdminPremiumPlanResponse(inactivePlan)).thenReturn(activePlanResponse);

            premiumAdminService.enablePlan(2L);

            assertThat(inactivePlan.getIsActive()).isTrue();
            verify(premiumPlanRepository).save(inactivePlan);
        }

        @Test
        @DisplayName("should throw PREMIUM_PLAN_ALREADY_ACTIVE when plan is already active")
        void shouldThrowAlreadyActive_whenAlreadyActive() {
            when(premiumPlanRepository.findById(1L)).thenReturn(Optional.of(activePlan));

            assertThatThrownBy(() -> premiumAdminService.enablePlan(1L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PREMIUM_PLAN_ALREADY_ACTIVE);

            verify(premiumPlanRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("searchTransactionsForAdmin")
    class SearchTransactionsForAdmin {

        @Test
        @DisplayName("should return paginated transaction responses")
        @SuppressWarnings("unchecked")
        void shouldReturnPageResponse_whenCalled() {
            AdminTransactionFilterRequest filter = new AdminTransactionFilterRequest(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20);
            PaymentTransaction tx = PaymentTransaction.builder().id(100L).user(testUser).plan(activePlan).build();
            AdminTransactionResponse item = new AdminTransactionResponse(
                    100L, "ELGO123", 10L, "john_doe", "john@elingo.com", 1L, "Pro Monthly",
                    null, new BigDecimal("99000.00"), "VND", PaymentTransactionStatus.PENDING, null, LocalDateTime.now()
            );

            when(paymentTransactionRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(tx), pageable, 1));
            when(premiumMapper.toAdminTransactionResponse(tx)).thenReturn(item);

            PageResponse<AdminTransactionResponse> result = premiumAdminService.searchTransactionsForAdmin(filter, pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getTransactionByIdForAdmin")
    class GetTransactionByIdForAdmin {

        @Test
        @DisplayName("should return detail response when transaction exists")
        void shouldReturnDetail_whenExists() {
            PaymentTransaction tx = PaymentTransaction.builder().id(100L).build();
            AdminTransactionDetailResponse detail = buildDetailResponse(100L, PaymentTransactionStatus.PENDING, null);
            when(paymentTransactionRepository.findDetailById(100L)).thenReturn(Optional.of(tx));
            when(premiumMapper.toAdminTransactionDetailResponse(tx)).thenReturn(detail);

            AdminTransactionDetailResponse result = premiumAdminService.getTransactionByIdForAdmin(100L);

            assertThat(result.id()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should throw PAYMENT_TRANSACTION_NOT_FOUND when transaction does not exist")
        void shouldThrowNotFound_whenDoesNotExist() {
            when(paymentTransactionRepository.findDetailById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> premiumAdminService.getTransactionByIdForAdmin(999L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PAYMENT_TRANSACTION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("confirmTransactionManually")
    class ConfirmTransactionManually {

        @Test
        @DisplayName("should confirm PENDING transaction, activate subscription, merge metadata, and publish events")
        void shouldConfirmPendingTransaction_whenValid() {
            PaymentTransaction tx = PaymentTransaction.builder()
                    .id(100L)
                    .user(testUser)
                    .plan(activePlan)
                    .amount(new BigDecimal("99000.00"))
                    .status(PaymentTransactionStatus.PENDING)
                    .metadata("{\"underpaid\":true}")
                    .build();
            UserSubscription sub = UserSubscription.builder()
                    .id(55L)
                    .user(testUser)
                    .plan(activePlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startAt(LocalDateTime.now())
                    .endAt(LocalDateTime.now().plusDays(30))
                    .build();
            TransactionReasonRequest request = new TransactionReasonRequest("Verified bank statement");
            AdminTransactionDetailResponse detail = buildDetailResponse(100L, PaymentTransactionStatus.SUCCESS, 55L);

            when(paymentTransactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(tx));
            when(subscriptionService.activateOrExtend(10L, 1L)).thenReturn(sub);
            when(paymentTransactionRepository.findDetailById(100L)).thenReturn(Optional.of(tx));
            when(premiumMapper.toAdminTransactionDetailResponse(tx)).thenReturn(detail);

            AdminTransactionDetailResponse result = premiumAdminService.confirmTransactionManually(1L, 100L, request);

            assertThat(result.status()).isEqualTo(PaymentTransactionStatus.SUCCESS);
            assertThat(tx.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCESS);
            assertThat(tx.getPaidAt()).isNotNull();
            assertThat(tx.getSubscription()).isEqualTo(sub);
            assertThat(tx.getMetadata()).contains("\"underpaid\":true", "\"manualConfirmed\":true", "\"confirmedByAdminId\":1");

            verify(paymentTransactionRepository).save(tx);
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getAllValues().get(0)).isInstanceOf(PaymentSucceededEvent.class);
            assertThat(eventCaptor.getAllValues().get(1)).isInstanceOf(NotificationEvent.class);
        }

        @Test
        @DisplayName("should preserve raw non-JSON metadata when confirming")
        void shouldPreserveRawMetadata_whenExistingMetadataIsInvalidJson() {
            PaymentTransaction tx = PaymentTransaction.builder()
                    .id(100L)
                    .user(testUser)
                    .plan(activePlan)
                    .amount(new BigDecimal("99000.00"))
                    .status(PaymentTransactionStatus.PENDING)
                    .metadata("not-json-string")
                    .build();
            UserSubscription sub = UserSubscription.builder()
                    .id(55L)
                    .user(testUser)
                    .plan(activePlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .endAt(LocalDateTime.now().plusDays(30))
                    .build();

            when(paymentTransactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(tx));
            when(subscriptionService.activateOrExtend(10L, 1L)).thenReturn(sub);
            when(paymentTransactionRepository.findDetailById(100L)).thenReturn(Optional.of(tx));
            when(premiumMapper.toAdminTransactionDetailResponse(tx))
                    .thenReturn(buildDetailResponse(100L, PaymentTransactionStatus.SUCCESS, 55L));

            premiumAdminService.confirmTransactionManually(1L, 100L, new TransactionReasonRequest("Manual OK"));

            assertThat(tx.getMetadata()).contains("\"previousRawMetadata\":\"not-json-string\"");
        }

        @Test
        @DisplayName("should throw PAYMENT_TRANSACTION_NOT_FOUND when transaction does not exist")
        void shouldThrowNotFound_whenTransactionMissing() {
            when(paymentTransactionRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> premiumAdminService.confirmTransactionManually(1L, 999L, new TransactionReasonRequest("Reason")))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PAYMENT_TRANSACTION_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw PAYMENT_TRANSACTION_ALREADY_PROCESSED when status is not PENDING")
        void shouldThrowAlreadyProcessed_whenNotPending() {
            PaymentTransaction tx = PaymentTransaction.builder()
                    .id(100L)
                    .status(PaymentTransactionStatus.SUCCESS)
                    .build();
            when(paymentTransactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> premiumAdminService.confirmTransactionManually(1L, 100L, new TransactionReasonRequest("Reason")))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PAYMENT_TRANSACTION_ALREADY_PROCESSED);

            verify(subscriptionService, never()).activateOrExtend(any(), any());
        }
    }

    @Nested
    @DisplayName("refundTransaction")
    class RefundTransaction {

        @Test
        @DisplayName("should refund SUCCESS transaction, revoke subscription time, and save metadata")
        void shouldRefundSuccessTransaction_whenValid() {
            LocalDateTime paidAt = LocalDateTime.now().minusDays(1);
            UserSubscription sub = UserSubscription.builder().id(55L).build();
            PaymentTransaction tx = PaymentTransaction.builder()
                    .id(100L)
                    .user(testUser)
                    .plan(activePlan)
                    .subscription(sub)
                    .status(PaymentTransactionStatus.SUCCESS)
                    .paidAt(paidAt)
                    .build();
            TransactionReasonRequest request = new TransactionReasonRequest("Customer refund request");

            when(paymentTransactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(tx));
            when(subscriptionService.revokeSubscriptionTime(10L, 55L, 30, paidAt)).thenReturn(Duration.ofDays(29));
            when(paymentTransactionRepository.findDetailById(100L)).thenReturn(Optional.of(tx));
            when(premiumMapper.toAdminTransactionDetailResponse(tx))
                    .thenReturn(buildDetailResponse(100L, PaymentTransactionStatus.REFUNDED, 55L));

            AdminTransactionDetailResponse result = premiumAdminService.refundTransaction(1L, 100L, request);

            assertThat(result.status()).isEqualTo(PaymentTransactionStatus.REFUNDED);
            assertThat(tx.getStatus()).isEqualTo(PaymentTransactionStatus.REFUNDED);
            assertThat(tx.getMetadata()).contains("\"refunded\":true", "\"refundedByAdminId\":1", "\"revokedMinutes\":41760");
            verify(paymentTransactionRepository).save(tx);
        }

        @Test
        @DisplayName("should throw PAYMENT_TRANSACTION_NOT_REFUNDABLE when status is not SUCCESS")
        void shouldThrowNotRefundable_whenNotSuccess() {
            PaymentTransaction tx = PaymentTransaction.builder()
                    .id(100L)
                    .status(PaymentTransactionStatus.PENDING)
                    .build();
            when(paymentTransactionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> premiumAdminService.refundTransaction(1L, 100L, new TransactionReasonRequest("Refund")))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PAYMENT_TRANSACTION_NOT_REFUNDABLE);

            verify(subscriptionService, never()).revokeSubscriptionTime(any(), any(), eq(30), any());
        }
    }

    @Nested
    @DisplayName("searchWebhookLogsForAdmin")
    class SearchWebhookLogsForAdmin {

        @Test
        @DisplayName("should return paginated webhook log summaries")
        @SuppressWarnings("unchecked")
        void shouldReturnPageResponse_whenCalled() {
            AdminWebhookLogFilterRequest filter = new AdminWebhookLogFilterRequest(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20);
            PaymentWebhookLog webhookLog = PaymentWebhookLog.builder()
                    .id(12L)
                    .provider(PaymentProvider.builder().id(1L).code("SEPAY").build())
                    .status(WebhookLogStatus.PROCESSED)
                    .build();
            AdminWebhookLogResponse item = new AdminWebhookLogResponse(
                    12L, 1L, "SEPAY", 100L, "evt_1", true, WebhookLogStatus.PROCESSED,
                    LocalDateTime.now(), LocalDateTime.now()
            );

            when(paymentWebhookLogRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(webhookLog), pageable, 1));
            when(premiumMapper.toAdminWebhookLogResponse(webhookLog)).thenReturn(item);

            PageResponse<AdminWebhookLogResponse> result = premiumAdminService.searchWebhookLogsForAdmin(filter, pageable);

            assertThat(result.content()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getWebhookLogByIdForAdmin")
    class GetWebhookLogByIdForAdmin {

        @Test
        @DisplayName("should return webhook log detail when exists")
        void shouldReturnDetail_whenExists() {
            PaymentWebhookLog webhookLog = PaymentWebhookLog.builder().id(12L).build();
            AdminWebhookLogDetailResponse detail = new AdminWebhookLogDetailResponse(
                    12L, 1L, "SEPAY", 100L, "evt_1", true, WebhookLogStatus.PROCESSED,
                    "{}", LocalDateTime.now(), LocalDateTime.now()
            );
            when(paymentWebhookLogRepository.findDetailById(12L)).thenReturn(Optional.of(webhookLog));
            when(premiumMapper.toAdminWebhookLogDetailResponse(webhookLog)).thenReturn(detail);

            AdminWebhookLogDetailResponse result = premiumAdminService.getWebhookLogByIdForAdmin(12L);

            assertThat(result.id()).isEqualTo(12L);
        }

        @Test
        @DisplayName("should throw PAYMENT_WEBHOOK_LOG_NOT_FOUND when does not exist")
        void shouldThrowNotFound_whenDoesNotExist() {
            when(paymentWebhookLogRepository.findDetailById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> premiumAdminService.getWebhookLogByIdForAdmin(99L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PAYMENT_WEBHOOK_LOG_NOT_FOUND);
        }
    }

    private AdminTransactionDetailResponse buildDetailResponse(Long txId, PaymentTransactionStatus status, Long subId) {
        return new AdminTransactionDetailResponse(
                txId, "ELGO123", 10L, "john_doe", "john@elingo.com", "John Doe",
                1L, "Pro Monthly", 30, null, null, subId,
                subId != null ? SubscriptionStatus.ACTIVE : null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                new BigDecimal("99000.00"), "VND", status, null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
