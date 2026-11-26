package com.elingo.premium.dto.response;

import com.elingo.premium.entity.PaymentTransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Summary payment transaction item for administration list")
public record AdminTransactionResponse(
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

        @Schema(description = "Premium plan ID", example = "1")
        Long planId,

        @Schema(description = "Premium plan name", example = "Pro Monthly")
        String planName,

        @Schema(description = "Payment provider code (null if not yet paid or manual)", example = "SEPAY")
        String providerCode,

        @Schema(description = "Transaction amount", example = "99000.00")
        BigDecimal amount,

        @Schema(description = "Currency code", example = "VND")
        String currency,

        @Schema(description = "Current transaction status", example = "SUCCESS")
        PaymentTransactionStatus status,

        @Schema(description = "Payment completion timestamp", example = "2026-11-25T10:15:30")
        LocalDateTime paidAt,

        @Schema(description = "Transaction creation timestamp", example = "2026-11-25T10:10:00")
        LocalDateTime createdAt
) {
}
