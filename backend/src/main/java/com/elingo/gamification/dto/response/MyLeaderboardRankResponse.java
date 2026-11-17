package com.elingo.gamification.dto.response;

import com.elingo.gamification.entity.LeaderboardPeriodType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current user's ranking information and progress towards the next rank")
public record MyLeaderboardRankResponse(
        @Schema(description = "Current rank of the user (null if not ranked yet)", example = "5")
        Integer rank,

        @Schema(description = "Total XP accumulated by the user in this period", example = "850")
        Integer totalXp,

        @Schema(description = "XP difference needed to overtake the user directly ahead (null if rank 1 or unranked)",
                example = "50")
        Integer diffToNextRank,

        @Schema(description = "Leaderboard period type", example = "WEEKLY")
        LeaderboardPeriodType periodType,

        @Schema(description = "Period key formatted according to period type (e.g., YYYY-'W'ww for weekly, YYYY-MM for monthly)", example = "2026-W46")
        String periodKey
) {
}
