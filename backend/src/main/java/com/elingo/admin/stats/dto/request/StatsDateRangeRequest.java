package com.elingo.admin.stats.dto.request;

import com.elingo.common.enums.StatsGranularity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Schema(description = "Date range and granularity filter request for statistics")
public record StatsDateRangeRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(description = "Start date (YYYY-MM-DD), defaults to 30 days ago", example = "2026-10-21")
        LocalDate from,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(description = "End date (YYYY-MM-DD), defaults to current date", example = "2026-11-20")
        LocalDate to,

        @Schema(description = "Time grouping granularity (DAY, WEEK, MONTH), defaults to DAY", example = "DAY")
        StatsGranularity granularity
) {
    public StatsDateRangeRequest {
        if (to == null) {
            to = LocalDate.now();
        }
        if (from == null) {
            from = to.minusDays(29);
        }
        if (granularity == null) {
            granularity = StatsGranularity.DAY;
        }
    }

    @Schema(hidden = true)
    @AssertTrue(message = "INVALID_DATE_RANGE")
    public boolean isDateRangeValid() {
        if (from.isAfter(to)) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return days <= maxDays();
    }

    public static StatsDateRangeRequest of(LocalDate from, LocalDate to, StatsGranularity granularity) {
        return new StatsDateRangeRequest(from, to, granularity);
    }

    private int maxDays() {
        return switch (granularity) {
            case DAY -> 90;
            case WEEK -> 180;
            case MONTH -> 400;
        };
    }
}
