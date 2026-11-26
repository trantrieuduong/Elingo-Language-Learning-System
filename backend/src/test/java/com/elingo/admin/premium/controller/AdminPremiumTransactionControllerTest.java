package com.elingo.admin.premium.controller;

import com.elingo.admin.premium.service.AdminPremiumTransactionService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.premium.dto.request.AdminTransactionFilterRequest;
import com.elingo.premium.dto.request.TransactionReasonRequest;
import com.elingo.premium.dto.response.AdminTransactionDetailResponse;
import com.elingo.premium.dto.response.AdminTransactionResponse;
import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminPremiumTransactionController")
class AdminPremiumTransactionControllerTest {

    @Mock
    private AdminPremiumTransactionService adminPremiumTransactionService;

    @InjectMocks
    private AdminPremiumTransactionController controller;

    private static final AdminTransactionDetailResponse SAMPLE_DETAIL = new AdminTransactionDetailResponse(
            100L, "ELGO123", 10L, "john_doe", "john@elingo.com", "John Doe",
            1L, "Pro Monthly", 30, null, null, 55L, SubscriptionStatus.ACTIVE,
            LocalDateTime.now(), LocalDateTime.now().plusDays(30),
            new BigDecimal("99000.00"), "VND", PaymentTransactionStatus.SUCCESS,
            null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
    );

    @Nested
    @DisplayName("searchTransactions")
    class SearchTransactions {

        @Test
        @DisplayName("should return paginated transaction list")
        void shouldSearchTransactions() {
            AdminTransactionFilterRequest filter = new AdminTransactionFilterRequest(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20);
            PageResponse<AdminTransactionResponse> page = new PageResponse<>(
                    List.of(), 1, 20, 0L, 0, true
            );

            when(adminPremiumTransactionService.searchTransactions(filter, pageable)).thenReturn(page);

            ApiResponse<PageResponse<AdminTransactionResponse>> response = controller.searchTransactions(filter, pageable);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(page);
            verify(adminPremiumTransactionService).searchTransactions(filter, pageable);
        }
    }

    @Nested
    @DisplayName("getTransactionById")
    class GetTransactionById {

        @Test
        @DisplayName("should return transaction details")
        void shouldGetTransactionById() {
            when(adminPremiumTransactionService.getTransactionById(100L)).thenReturn(SAMPLE_DETAIL);

            ApiResponse<AdminTransactionDetailResponse> response = controller.getTransactionById(100L);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_DETAIL);
        }
    }

    @Nested
    @DisplayName("confirmManually")
    class ConfirmManually {

        @Test
        @DisplayName("should confirm transaction manually")
        void shouldConfirmManually() {
            TransactionReasonRequest request = new TransactionReasonRequest("Verified transfer");
            when(adminPremiumTransactionService.confirmManually(1L, 100L, request)).thenReturn(SAMPLE_DETAIL);

            ApiResponse<AdminTransactionDetailResponse> response = controller.confirmManually(1L, 100L, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_DETAIL);
            verify(adminPremiumTransactionService).confirmManually(1L, 100L, request);
        }
    }

    @Nested
    @DisplayName("refundTransaction")
    class RefundTransaction {

        @Test
        @DisplayName("should refund transaction")
        void shouldRefundTransaction() {
            TransactionReasonRequest request = new TransactionReasonRequest("Refund request");
            when(adminPremiumTransactionService.refundTransaction(1L, 100L, request)).thenReturn(SAMPLE_DETAIL);

            ApiResponse<AdminTransactionDetailResponse> response = controller.refundTransaction(1L, 100L, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_DETAIL);
            verify(adminPremiumTransactionService).refundTransaction(1L, 100L, request);
        }
    }
}
