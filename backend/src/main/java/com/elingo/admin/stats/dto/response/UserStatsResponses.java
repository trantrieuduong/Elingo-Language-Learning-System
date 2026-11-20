package com.elingo.admin.stats.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public final class UserStatsResponses {
    private UserStatsResponses() {}

    @Schema(description = "Generic key-value data point for time-series or categorized statistics")
    public record StatPoint<T, K>(
            @Schema(description = "Period key or category identifier (e.g., date YYYY-MM-DD or level number)", example = "2026-11-20")
            T key,
            @Schema(description = "Metric count or value for this data point", example = "42")
            K value
    ) {}

    @Schema(description = "User growth statistics including periodic new users and cumulative totals")
    public record UserGrowthResponse(
            @Schema(description = "Total registered users prior to the query start date", example = "1520")
            Long totalBeforeRange,

            @Schema(description = "Continuous time-series list of newly registered users per period with zero-filled gaps")
            List<StatPoint<String, Long>> newUsers,

            @Schema(description = "Continuous time-series list of running cumulative total users")
            List<StatPoint<String, Long>> cumulativeUsers,

            @Schema(description = "Timestamp when this statistic report was generated", example = "2026-11-20T21:00:00")
            LocalDateTime generatedAt
    ) {}

    @Schema(description = "User activity statistics covering DAU time-series, WAU, MAU, and stickiness ratio")
    public record UserActivityResponse(
            @Schema(description = "Daily active users on the latest date in the range (distinct active learners in xp_events)", example = "125")
            Long currentDau,

            @Schema(description = "Weekly active users (distinct active learners in xp_events over trailing 7 days)", example = "480")
            Long currentWau,

            @Schema(description = "Monthly active users (distinct active learners in xp_events over trailing 30 days)", example = "1650")
            Long currentMau,

            @Schema(description = "User engagement stickiness ratio as a percentage (DAU / MAU * 100)", example = "7.58")
            Double stickinessPercentage,

            @Schema(description = "Continuous daily active users series with missing dates filled as 0")
            List<StatPoint<String, Long>> dauTimeSeries,

            @Schema(description = "Timestamp when this statistic report was generated", example = "2026-11-20T21:00:00")
            LocalDateTime generatedAt
    ) {}

    @Schema(description = "Overall breakdown of system users across verification, account status, subscription tier, and level distribution")
    public record UserBreakdownResponse(
            @Schema(description = "Total registered users across the entire system", example = "2500")
            Long totalUsers,

            @Schema(description = "Number of users with verified email address", example = "2150")
            Long verifiedCount,

            @Schema(description = "Percentage of users verified", example = "86.0")
            Double verifiedPercentage,

            @Schema(description = "Number of active (non-banned) users", example = "2450")
            Long activeCount,

            @Schema(description = "Percentage of active users", example = "98.0")
            Double activePercentage,

            @Schema(description = "Number of distinct users currently holding an active Premium subscription", example = "350")
            Long premiumCount,

            @Schema(description = "Percentage of users holding Premium subscription", example = "14.0")
            Double premiumPercentage,

            @Schema(description = "Distribution of users by CEFR/gamification level number")
            List<StatPoint<Short, Long>> levelDistribution,

            @Schema(description = "Timestamp when this statistic report was generated", example = "2026-11-20T21:00:00")
            LocalDateTime generatedAt
    ) {}
}
