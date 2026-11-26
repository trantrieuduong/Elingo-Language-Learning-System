package com.elingo.premium.dto.response;

import com.elingo.premium.entity.WebhookLogStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Summary payment webhook log item for administration list")
public record AdminWebhookLogResponse(
        @Schema(description = "Webhook log ID", example = "12")
        Long id,

        @Schema(description = "Payment provider ID", example = "1")
        Long providerId,

        @Schema(description = "Payment provider code", example = "SEPAY")
        String providerCode,

        @Schema(description = "Matched payment transaction ID (null if unmatched)", example = "100")
        Long transactionId,

        @Schema(description = "External event ID from provider", example = "sepay_evt_123456")
        String eventId,

        @Schema(description = "Whether the webhook signature/token was valid", example = "true")
        Boolean signatureValid,

        @Schema(description = "Webhook processing status", example = "PROCESSED")
        WebhookLogStatus status,

        @Schema(description = "Timestamp when webhook was received", example = "2026-11-25T10:15:29")
        LocalDateTime receivedAt,

        @Schema(description = "Timestamp when webhook finished processing", example = "2026-11-25T10:15:30")
        LocalDateTime processedAt
) {
}
