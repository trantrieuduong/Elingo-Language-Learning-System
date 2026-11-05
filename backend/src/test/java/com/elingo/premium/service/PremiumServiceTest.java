package com.elingo.premium.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.config.PremiumProperties;
import com.elingo.premium.dto.PaymentQrInfo;
import com.elingo.premium.dto.request.CreateCheckoutRequest;
import com.elingo.premium.dto.response.CheckoutResponse;
import com.elingo.premium.dto.response.MySubscriptionResponse;
import com.elingo.premium.dto.response.PaymentStatusResponse;
import com.elingo.premium.dto.response.PaymentTransactionResponse;
import com.elingo.premium.dto.response.PremiumPlanResponse;
import com.elingo.premium.entity.PaymentTransaction;
import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.PremiumPlan;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.mapper.PremiumMapper;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.repository.PremiumPlanRepository;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.premium.service.impl.PremiumServiceImpl;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PremiumServiceTest {

    @Mock
    private PremiumPlanRepository premiumPlanRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PremiumMapper premiumMapper;

    @Mock
    private PaymentQrService paymentQrService;

    @Mock
    private PremiumProperties premiumProperties;

    @InjectMocks
    private PremiumServiceImpl premiumService;

    private User testUser;
    private PremiumPlan testPlan;
    private PremiumProperties.BankTransfer bankTransferConfig;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("testuser").build();
        testPlan = PremiumPlan.builder()
                .id(10L)
                .name("Gói 1 tháng")
                .price(new BigDecimal("99000"))
                .currency("VND")
                .durationDays(30)
                .isActive(true)
                .build();

        bankTransferConfig = new PremiumProperties.BankTransfer();
        bankTransferConfig.setCodePrefix("ELGO");
        bankTransferConfig.setBankName("MBBank");
        bankTransferConfig.setBankAccount("0123456789");
        bankTransferConfig.setBankAccountName("CONG TY ELINGO");
    }

    @Nested
    @DisplayName("checkout tests")
    class CheckoutTests {

        @Test
        @DisplayName("Should create checkout transaction successfully and return QR info")
        void checkout_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(premiumPlanRepository.findById(10L)).thenReturn(Optional.of(testPlan));
            when(premiumProperties.getBankTransfer()).thenReturn(bankTransferConfig);

            when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                    .thenAnswer(invocation -> {
                        PaymentTransaction tx = invocation.getArgument(0);
                        tx.setId(100L);
                        return tx;
                    });

            PaymentQrInfo qrInfo = new PaymentQrInfo(
                    "https://vietqr.app/img?acc=123",
                    "MBBank",
                    "0123456789",
                    "ELINGO"
            );
            when(paymentQrService.generateQrInfo(any(), any(), any())).thenReturn(qrInfo);

            CreateCheckoutRequest request = new CreateCheckoutRequest(10L);
            CheckoutResponse response = premiumService.checkout(1L, request);

            assertThat(response).isNotNull();
            assertThat(response.transactionId()).isEqualTo(100L);
            assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("99000"));
            assertThat(response.qrCodeUrl()).isEqualTo("https://vietqr.app/img?acc=123");
            assertThat(response.bankName()).isEqualTo("MBBank");
            assertThat(response.bankAccount()).isEqualTo("0123456789");
            assertThat(response.bankAccountName()).isEqualTo("ELINGO");
            assertThat(response.providerRefCode()).startsWith("ELGO");

            verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when user does not exist")
        void checkout_ThrowsException_WhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            CreateCheckoutRequest request = new CreateCheckoutRequest(10L);

            assertThatThrownBy(() -> premiumService.checkout(999L, request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw PREMIUM_PLAN_NOT_FOUND when plan does not exist")
        void checkout_ThrowsException_WhenPlanNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(premiumPlanRepository.findById(999L)).thenReturn(Optional.empty());

            CreateCheckoutRequest request = new CreateCheckoutRequest(999L);

            assertThatThrownBy(() -> premiumService.checkout(1L, request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PREMIUM_PLAN_NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw PREMIUM_PLAN_INACTIVE when plan is inactive")
        void checkout_ThrowsException_WhenPlanInactive() {
            testPlan.setIsActive(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(premiumPlanRepository.findById(10L)).thenReturn(Optional.of(testPlan));

            CreateCheckoutRequest request = new CreateCheckoutRequest(10L);

            assertThatThrownBy(() -> premiumService.checkout(1L, request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PREMIUM_PLAN_INACTIVE);
        }
    }

    @Nested
    @DisplayName("getTransactionStatus tests")
    class GetTransactionStatusTests {

        @Test
        @DisplayName("Should return transaction status successfully when transaction exists and belongs to user")
        void getTransactionStatus_ReturnsStatus_WhenFound() {
            PaymentTransaction tx = PaymentTransaction.builder()
                    .id(100L)
                    .providerRefCode("ELGO_REF_100")
                    .status(PaymentTransactionStatus.SUCCESS)
                    .paidAt(LocalDateTime.now())
                    .plan(testPlan)
                    .build();

            when(paymentTransactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(tx));

            PaymentStatusResponse response = premiumService.getTransactionStatus(1L, 100L);

            assertThat(response).isNotNull();
            assertThat(response.transactionId()).isEqualTo(100L);
            assertThat(response.status()).isEqualTo(PaymentTransactionStatus.SUCCESS);
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.planName()).isEqualTo("Gói 1 tháng");
        }

        @Test
        @DisplayName("Should throw PAYMENT_TRANSACTION_NOT_FOUND when transaction does not exist or does not belong to user")
        void getTransactionStatus_ThrowsException_WhenNotFound() {
            when(paymentTransactionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> premiumService.getTransactionStatus(1L, 999L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PAYMENT_TRANSACTION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getMySubscription tests")
    class GetMySubscriptionTests {

        @Test
        @DisplayName("Should return empty subscription info when user has no subscriptions")
        void getMySubscription_ReturnsEmpty_WhenNoSubscription() {
            when(userSubscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

            MySubscriptionResponse response = premiumService.getMySubscription(1L);

            assertThat(response).isNotNull();
            assertThat(response.status()).isNull();
            assertThat(response.planName()).isNull();
            assertThat(response.remainingDays()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should return active plan info and calculate correct remaining days when user has active subscription")
        void getMySubscription_ReturnsPlanAndRemainingDays_WhenActive() {
            LocalDateTime futureEndAt = LocalDateTime.now().plusDays(15);
            UserSubscription sub = UserSubscription.builder()
                    .plan(testPlan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startAt(LocalDateTime.now().minusDays(15))
                    .endAt(futureEndAt)
                    .build();

            when(userSubscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(sub));

            MySubscriptionResponse response = premiumService.getMySubscription(1L);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(response.planName()).isEqualTo("Gói 1 tháng");
            assertThat(response.remainingDays()).isGreaterThanOrEqualTo(14L);
        }
    }

    @Nested
    @DisplayName("getActivePlans & getMyTransactions tests")
    class OtherEndpointsTests {

        @Test
        @DisplayName("Should return list of active premium plans")
        void getActivePlans_ReturnsPlans() {
            when(premiumPlanRepository.findAllByIsActiveTrueOrderByPriceAsc()).thenReturn(List.of(testPlan));
            PremiumPlanResponse planResponse = new PremiumPlanResponse(10L, "Gói 1 tháng", new BigDecimal("99000"), "VND", 30);
            when(premiumMapper.toPremiumPlanResponseList(List.of(testPlan))).thenReturn(List.of(planResponse));

            List<PremiumPlanResponse> result = premiumService.getActivePlans();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Gói 1 tháng");
        }

        @Test
        @DisplayName("Should return paginated list of user's payment transactions")
        void getMyTransactions_ReturnsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            PaymentTransaction tx = PaymentTransaction.builder()
                    .id(1L)
                    .providerRefCode("ELGO_TX1")
                    .plan(testPlan)
                    .amount(new BigDecimal("99000"))
                    .currency("VND")
                    .status(PaymentTransactionStatus.SUCCESS)
                    .build();

            Page<PaymentTransaction> page = new PageImpl<>(List.of(tx), pageable, 1);
            when(paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);

            PageResponse<PaymentTransactionResponse> result = premiumService.getMyTransactions(1L, pageable);

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).providerRefCode()).isEqualTo("ELGO_TX1");
        }
    }
}
