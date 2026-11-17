package com.elingo.gamification.dto.response;

import com.elingo.common.dto.PageResponse;
import com.elingo.gamification.entity.LeaderboardPeriodType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Realtime leaderboard response containing paginated user rankings and current user rank")
public record LeaderboardResponse(
        @Schema(description = "Leaderboard period type (WEEKLY or MONTHLY)", example = "WEEKLY")
        LeaderboardPeriodType periodType,

        @Schema(description = "Current period key (e.g., '2026-W46' for weekly, '2026-11' for monthly)",
                example = "2026-W46")
        String periodKey,

        @Schema(description = "Paginated ranking items conforming to system PageResponse standard")
        PageResponse<LeaderboardItemResponse> leaderboard,

        @Schema(description = "Rank details of the authenticated user in this period")
        MyLeaderboardRankResponse myRank
) {
}
