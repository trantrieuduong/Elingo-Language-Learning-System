package com.elingo.admin.premium.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.premium.dto.request.AdminTransactionFilterRequest;
import com.elingo.premium.dto.request.TransactionReasonRequest;
import com.elingo.premium.dto.response.AdminTransactionDetailResponse;
import com.elingo.premium.dto.response.AdminTransactionResponse;
import org.springframework.data.domain.Pageable;

public interface AdminPremiumTransactionService {
    PageResponse<AdminTransactionResponse> searchTransactions(AdminTransactionFilterRequest filter, Pageable pageable);
    AdminTransactionDetailResponse getTransactionById(Long transactionId);
    AdminTransactionDetailResponse confirmManually(Long adminId, Long transactionId, TransactionReasonRequest request);
    AdminTransactionDetailResponse refundTransaction(Long adminId, Long transactionId, TransactionReasonRequest request);
}
