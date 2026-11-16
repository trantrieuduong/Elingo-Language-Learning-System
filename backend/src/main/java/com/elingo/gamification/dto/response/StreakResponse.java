package com.elingo.gamification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "User learning streak and monthly check-in history response")
public record StreakResponse(
        @Schema(description = "ID of the user", example = "1")
        Long userId,

        @Schema(description = "Current consecutive learning streak in days", example = "5")
        Integer currentStreak,

        @Schema(description = "Longest consecutive learning streak achieved in days", example = "14")
        Integer longestStreak,

        @Schema(description = "Most recent check-in date", example = "2026-11-16")
        LocalDate lastActiveDate,

        @Schema(description = "Whether the user has checked in today", example = "true")
        Boolean checkedInToday,

        @Schema(description = "Total number of check-ins in the queried month", example = "12")
        Integer monthCheckinCount,

        @Schema(description = "List of dates the user checked in during the queried month", example = "[\"2026-11-01\", \"2026-11-02\", \"2026-11-16\"]")
        List<LocalDate> checkinDates
) {
}
