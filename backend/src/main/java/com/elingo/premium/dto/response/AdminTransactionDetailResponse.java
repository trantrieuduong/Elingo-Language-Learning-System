package com.elingo.premium.dto.response;

import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Detailed payment transaction information for administration")
public record AdminTransactionDetailResponse(
        @Schema(description = "Transaction ID", example = "100")
        Long id,

        @Schema(description = "Unique transfer reference code", example = "ELGO8A9B1C2D3E")
        String providerRefCode,

        @Schema(description = "User ID", example = "10")
        Long userId,

        @Schema(description = "Username", example = "john_doe")
        String username,

        @Schema(description = "User email", example = "john.doe@example.com")
        String userEmail,

        @Schema(description = "User full name", example = "John Doe")
        String userFullName,

        @Schema(description = "Premium plan ID", example = "1")
        Long planId,

        @Schema(description = "Premium plan name", example = "Pro Monthly")
        String planName,

        @Schema(description = "Plan duration in days", example = "30")
        Integer planDurationDays,

        @Schema(description = "Payment provider code", example = "SEPAY")
        String providerCode,

        @Schema(description = "Payment provider display name", example = "SePay")
        String providerName,

        @Schema(description = "Linked subscription ID", example = "55")
        Long subscriptionId,

        @Schema(description = "Linked subscription status", example = "ACTIVE")
        SubscriptionStatus subscriptionStatus,

        @Schema(description = "Linked subscription start timestamp", example = "2026-11-25T10:15:30")
        LocalDateTime subscriptionStartAt,

        @Schema(description = "Linked subscription end timestamp", example = "2026-12-25T10:15:30")
        LocalDateTime subscriptionEndAt,

        @Schema(description = "Transaction amount", example = "99000.00")
        BigDecimal amount,

        @Schema(description = "Currency code", example = "VND")
        String currency,

        @Schema(description = "Current transaction status", example = "SUCCESS")
        PaymentTransactionStatus status,

        @Schema(description = "Raw JSON metadata for gateway reconciliation, manual confirmation, or refund")
        String metadata,

        @Schema(description = "Payment completion timestamp", example = "2026-11-25T10:15:30")
        LocalDateTime paidAt,

        @Schema(description = "Transaction creation timestamp", example = "2026-11-25T10:10:00")
        LocalDateTime createdAt,

        @Schema(description = "Transaction last update timestamp", example = "2026-11-25T10:15:30")
        LocalDateTime updatedAt
) {
}
