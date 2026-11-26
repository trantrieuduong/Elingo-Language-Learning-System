package com.elingo.premium.service;

import com.elingo.common.dto.PageResponse;
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
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PremiumAdminService {
    // Plans
    List<AdminPremiumPlanResponse> getAllPlansForAdmin();
    AdminPremiumPlanResponse getPlanByIdForAdmin(Long planId);
    AdminPremiumPlanResponse createPlan(CreatePremiumPlanRequest request);
    AdminPremiumPlanResponse updatePlan(Long planId, UpdatePremiumPlanRequest request);
    AdminPremiumPlanResponse disablePlan(Long planId);
    AdminPremiumPlanResponse enablePlan(Long planId);

    // Transactions
    PageResponse<AdminTransactionResponse> searchTransactionsForAdmin(AdminTransactionFilterRequest filter, Pageable pageable);
    AdminTransactionDetailResponse getTransactionByIdForAdmin(Long transactionId);
    AdminTransactionDetailResponse confirmTransactionManually(Long adminId, Long transactionId, TransactionReasonRequest request);
    AdminTransactionDetailResponse refundTransaction(Long adminId, Long transactionId, TransactionReasonRequest request);

    // Webhook logs
    PageResponse<AdminWebhookLogResponse> searchWebhookLogsForAdmin(AdminWebhookLogFilterRequest filter, Pageable pageable);
    AdminWebhookLogDetailResponse getWebhookLogByIdForAdmin(Long logId);
}
