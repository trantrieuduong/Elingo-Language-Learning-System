package com.elingo.report.dto.request;

import com.elingo.report.entity.ReportStatus;
import com.elingo.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Schema(description = "Request query parameters for searching and filtering user reports in the admin console")
public record AdminReportSearchRequest(
        @Schema(description = "Filter by report status", example = "PENDING")
        ReportStatus status,

        @Schema(description = "Filter by reported target type", example = "POST")
        ReportTargetType targetType,

        @Schema(description = "Filter by reporter user ID", example = "12")
        @Positive(message = "INVALID_REQUEST")
        Long reporterId,

        @Schema(description = "Filter by reported target ID", example = "105")
        @Positive(message = "INVALID_REQUEST")
        Long targetId,

        @Schema(description = "Filter reports created on or after this date (ISO format: YYYY-MM-DD)", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdFrom,

        @Schema(description = "Filter reports created on or before this date (ISO format: YYYY-MM-DD)", example = "2026-12-31")
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
