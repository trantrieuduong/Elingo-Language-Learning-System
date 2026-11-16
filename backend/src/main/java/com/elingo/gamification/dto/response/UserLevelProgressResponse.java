package com.elingo.gamification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed level progress information for a user")
public record UserLevelProgressResponse(
        @Schema(description = "User ID", example = "1")
        Long userId,

        @Schema(description = "Current level number", example = "2")
        Short currentLevel,

        @Schema(description = "Title of current level", example = "Explorer")
        String currentTitle,

        @Schema(description = "Total experience points accumulated", example = "150")
        Integer totalXp,

        @Schema(description = "XP threshold of current level", example = "100")
        Integer currentLevelRequiredXp,

        @Schema(description = "Next level number (null if already at max level)", example = "3")
        Short nextLevel,

        @Schema(description = "Title of next level (null if already at max level)", example = "Challenger")
        String nextLevelTitle,

        @Schema(description = "XP threshold to reach next level (null if already at max level)", example = "300")
        Integer nextLevelRequiredXp,

        @Schema(description = "Remaining XP needed to reach next level (0 if at max level)", example = "150")
        Integer xpToNextLevel,

        @Schema(description = "Progress percentage towards next level (0.0 to 100.0)", example = "25.0")
        Double progressPercent
) {
}
