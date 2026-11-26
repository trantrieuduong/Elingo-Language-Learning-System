package com.elingo.premium.dto.request;

import com.elingo.premium.entity.WebhookLogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Schema(description = "Query parameters for filtering payment webhook logs in the admin console")
public record AdminWebhookLogFilterRequest(
        @Schema(description = "Filter by payment provider ID", example = "1")
        @Positive(message = "INVALID_REQUEST")
        Long providerId,

        @Schema(description = "Filter by matched payment transaction ID", example = "42")
        @Positive(message = "INVALID_REQUEST")
        Long transactionId,

        @Schema(description = "Filter by webhook log processing status", example = "IGNORED")
        WebhookLogStatus status,

        @Schema(description = "Filter by webhook signature validation result", example = "true")
        Boolean signatureValid,

        @Schema(description = "Filter logs received on or after this date (YYYY-MM-DD)", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate receivedFrom,

        @Schema(description = "Filter logs received on or before this date (YYYY-MM-DD)", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate receivedTo
) {
    @Schema(hidden = true)
    @AssertTrue(message = "INVALID_DATE_RANGE")
    public boolean isValidDateRange() {
        if (receivedFrom == null || receivedTo == null) {
            return true;
        }
        return !receivedFrom.isAfter(receivedTo) && ChronoUnit.DAYS.between(receivedFrom, receivedTo) <= 366;
    }
}
