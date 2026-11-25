package com.elingo.admin.premium.service;

import com.elingo.admin.premium.service.impl.AdminPremiumTransactionServiceImpl;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.dto.request.AdminTransactionFilterRequest;
import com.elingo.premium.dto.request.TransactionReasonRequest;
import com.elingo.premium.dto.response.AdminTransactionDetailResponse;
import com.elingo.premium.dto.response.AdminTransactionResponse;
import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.service.PremiumAdminService;
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
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminPremiumTransactionService")
class AdminPremiumTransactionServiceTest {

    @Mock
    private PremiumAdminService premiumAdminService;

    @InjectMocks
    private AdminPremiumTransactionServiceImpl adminPremiumTransactionService;

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
        @DisplayName("should delegate search with allowed sort field")
        void shouldSearchTransactions_whenSortIsAllowed() {
            AdminTransactionFilterRequest filter = new AdminTransactionFilterRequest(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            PageResponse<AdminTransactionResponse> expected = new PageResponse<>(
                    List.of(), 1, 20, 0L, 0, true
            );

            when(premiumAdminService.searchTransactionsForAdmin(filter, pageable)).thenReturn(expected);

            PageResponse<AdminTransactionResponse> result = adminPremiumTransactionService.searchTransactions(filter, pageable);

            assertThat(result).isEqualTo(expected);
            verify(premiumAdminService).searchTransactionsForAdmin(filter, pageable);
        }

        @Test
        @DisplayName("should throw INVALID_SORT when sort field is not whitelisted")
        void shouldThrowInvalidSort_whenSortNotAllowed() {
            AdminTransactionFilterRequest filter = new AdminTransactionFilterRequest(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20, Sort.by("unknownField"));

            assertThatThrownBy(() -> adminPremiumTransactionService.searchTransactions(filter, pageable))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.INVALID_SORT);

            verify(premiumAdminService, never()).searchTransactionsForAdmin(any(), any());
        }
    }

    @Nested
    @DisplayName("getTransactionById")
    class GetTransactionById {

        @Test
        @DisplayName("should return transaction details")
        void shouldReturnDetails_whenCalled() {
            when(premiumAdminService.getTransactionByIdForAdmin(100L)).thenReturn(SAMPLE_DETAIL);

            AdminTransactionDetailResponse result = adminPremiumTransactionService.getTransactionById(100L);

            assertThat(result).isEqualTo(SAMPLE_DETAIL);
        }
    }

    @Nested
    @DisplayName("confirmManually")
    class ConfirmManually {

        @Test
        @DisplayName("should delegate manual confirmation to premiumAdminService")
        void shouldConfirmManually_whenCalled() {
            TransactionReasonRequest request = new TransactionReasonRequest("Verified transfer");
            when(premiumAdminService.confirmTransactionManually(1L, 100L, request)).thenReturn(SAMPLE_DETAIL);

            AdminTransactionDetailResponse result = adminPremiumTransactionService.confirmManually(1L, 100L, request);

            assertThat(result).isEqualTo(SAMPLE_DETAIL);
            verify(premiumAdminService).confirmTransactionManually(1L, 100L, request);
        }
    }

    @Nested
    @DisplayName("refundTransaction")
    class RefundTransaction {

        @Test
        @DisplayName("should delegate refund to premiumAdminService")
        void shouldRefundTransaction_whenCalled() {
            TransactionReasonRequest request = new TransactionReasonRequest("Customer requested refund");
            when(premiumAdminService.refundTransaction(1L, 100L, request)).thenReturn(SAMPLE_DETAIL);

            AdminTransactionDetailResponse result = adminPremiumTransactionService.refundTransaction(1L, 100L, request);

            assertThat(result).isEqualTo(SAMPLE_DETAIL);
            verify(premiumAdminService).refundTransaction(1L, 100L, request);
        }
    }
}
