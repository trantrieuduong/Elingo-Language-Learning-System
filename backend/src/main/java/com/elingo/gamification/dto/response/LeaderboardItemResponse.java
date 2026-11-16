package com.elingo.gamification.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Leaderboard ranking entry for a user")
public record LeaderboardItemResponse(
        @Schema(description = "Rank position on the leaderboard (1-based)", example = "1")
        Integer rank,

        @Schema(description = "Unique ID of the user", example = "10")
        Long userId,

        @Schema(description = "Username of the user", example = "alex_master")
        String username,

        @Schema(description = "Avatar URL of the user",
                example = "https://cdn.elingo.com/avatars/user10.png")
        @BuildMediaUrl String avatarUrl,

        @Schema(description = "Total XP accumulated in this leaderboard period", example = "1250")
        Integer totalXp
) {
}
