package com.elingo.premium.dto.request;

import com.elingo.premium.entity.PaymentTransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Schema(description = "Query parameters for filtering payment transactions in the admin console")
public record AdminTransactionFilterRequest(
        @Schema(
                description = "Keyword to search by providerRefCode, username, email, or full name",
                example = "ELGO",
                maxLength = 100
        )
        @Size(max = 100, message = "INVALID_KEYWORD")
        String keyword,

        @Schema(description = "Filter by transaction status", example = "PENDING")
        PaymentTransactionStatus status,

        @Schema(description = "Filter by premium plan ID", example = "1")
        @Positive(message = "INVALID_REQUEST")
        Long planId,

        @Schema(description = "Filter by user ID", example = "10")
        @Positive(message = "INVALID_REQUEST")
        Long userId,

        @Schema(description = "Filter transactions created on or after this date (YYYY-MM-DD)", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdFrom,

        @Schema(description = "Filter transactions created on or before this date (YYYY-MM-DD)", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdTo
) {
    @Schema(hidden = true)
    @AssertTrue(message = "INVALID_DATE_RANGE")
    public boolean isValidDateRange() {
        if (createdFrom == null || createdTo == null) {
            return true;
        }
        return !createdFrom.isAfter(createdTo) && ChronoUnit.DAYS.between(createdFrom, createdTo) <= 366;
    }
}
