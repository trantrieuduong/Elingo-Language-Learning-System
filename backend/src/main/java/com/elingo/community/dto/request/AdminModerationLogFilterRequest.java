package com.elingo.community.dto.request;

import com.elingo.community.entity.ModerationAction;
import com.elingo.community.entity.ModerationActorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Schema(description = "Request query parameters for filtering post moderation logs")
public record AdminModerationLogFilterRequest(
        @Schema(description = "Filter by post ID", example = "105")
        @Positive(message = "INVALID_REQUEST")
        Long postId,

        @Schema(description = "Filter by actor type (AI or ADMIN)", example = "AI")
        ModerationActorType actorType,

        @Schema(description = "Filter by moderation action", example = "FLAG_FOR_REVIEW")
        ModerationAction action,

        @Schema(description = "Filter by AI classification label", example = "SPAM", maxLength = 100)
        @Size(max = 100, message = "INVALID_REQUEST")
        String aiLabel,

        @Schema(description = "Filter logs created on or after this date (ISO format: YYYY-MM-DD)", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdFrom,

        @Schema(description = "Filter logs created on or before this date (ISO format: YYYY-MM-DD)", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdTo
) {
    private static final long MAX_RANGE_DAYS = 366;

    @Schema(hidden = true)
    @AssertTrue(message = "INVALID_DATE_RANGE")
    public boolean isValidDateRange() {
        if (createdFrom == null || createdTo == null) {
            return true;
        }
        return !createdFrom.isAfter(createdTo)
                && ChronoUnit.DAYS.between(createdFrom, createdTo) <= MAX_RANGE_DAYS;
    }
}
