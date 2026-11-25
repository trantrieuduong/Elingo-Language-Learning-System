package com.elingo.admin.premium.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.common.util.AdminPageables;
import com.elingo.admin.premium.service.AdminPremiumTransactionService;
import com.elingo.common.dto.PageResponse;
import com.elingo.premium.dto.request.AdminTransactionFilterRequest;
import com.elingo.premium.dto.request.TransactionReasonRequest;
import com.elingo.premium.dto.response.AdminTransactionDetailResponse;
import com.elingo.premium.dto.response.AdminTransactionResponse;
import com.elingo.premium.service.PremiumAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-PREMIUM-TRANSACTION-SERVICE")
public class AdminPremiumTransactionServiceImpl implements AdminPremiumTransactionService {

    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "paidAt", "amount", "id");

    private final PremiumAdminService premiumAdminService;

    @Override
    public PageResponse<AdminTransactionResponse> searchTransactions(
            AdminTransactionFilterRequest filter,
            Pageable pageable
    ) {
        return premiumAdminService.searchTransactionsForAdmin(
                filter,
                AdminPageables.sanitize(pageable, ALLOWED_SORTS)
        );
    }

    @Override
    public AdminTransactionDetailResponse getTransactionById(Long transactionId) {
        return premiumAdminService.getTransactionByIdForAdmin(transactionId);
    }

    @Override
    @Audited(
            action = AdminAuditAction.TRANSACTION_MANUAL_CONFIRM,
            target = AdminAuditTargetType.TRANSACTION,
            targetId = "#transactionId",
            detail = "#request.reason()"
    )
    public AdminTransactionDetailResponse confirmManually(
            Long adminId,
            Long transactionId,
            TransactionReasonRequest request
    ) {
        AdminTransactionDetailResponse confirmed = premiumAdminService.confirmTransactionManually(
                adminId,
                transactionId,
                request
        );
        log.info("Payment transaction manually confirmed txId={} targetUserId={} subscriptionId={}",
                transactionId, confirmed.userId(), confirmed.subscriptionId());
        return confirmed;
    }

    @Override
    @Audited(
            action = AdminAuditAction.TRANSACTION_REFUND,
            target = AdminAuditTargetType.TRANSACTION,
            targetId = "#transactionId",
            detail = "#request.reason()"
    )
    public AdminTransactionDetailResponse refundTransaction(
            Long adminId,
            Long transactionId,
            TransactionReasonRequest request
    ) {
        AdminTransactionDetailResponse refunded = premiumAdminService.refundTransaction(
                adminId,
                transactionId,
                request
        );
        log.info("Payment transaction refunded txId={} targetUserId={} subscriptionId={}",
                transactionId, refunded.userId(), refunded.subscriptionId());
        return refunded;
    }
}
