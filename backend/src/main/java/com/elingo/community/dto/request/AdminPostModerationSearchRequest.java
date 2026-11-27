package com.elingo.community.dto.request;

import com.elingo.community.entity.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Schema(description = "Request query parameters for searching posts in the admin moderation queue")
public record AdminPostModerationSearchRequest(
        @Schema(description = "Filter by post status (e.g. NEEDS_REVIEW, PENDING, PUBLISHED, REJECTED, DELETED)", example = "NEEDS_REVIEW")
        PostStatus status,

        @Schema(description = "Filter by post group ID", example = "1")
        @Positive(message = "INVALID_REQUEST")
        Long groupId,

        @Schema(description = "Filter by author user ID", example = "15")
        @Positive(message = "INVALID_REQUEST")
        Long userId,

        @Schema(description = "Keyword to search in post title", example = "grammar", maxLength = 100)
        @Size(max = 100, message = "INVALID_KEYWORD")
        String keyword,

        @Schema(description = "Filter posts created on or after this date (ISO format: YYYY-MM-DD)", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdFrom,

        @Schema(description = "Filter posts created on or before this date (ISO format: YYYY-MM-DD)", example = "2026-12-31")
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
