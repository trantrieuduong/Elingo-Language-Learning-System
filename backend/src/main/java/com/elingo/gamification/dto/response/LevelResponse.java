package com.elingo.gamification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Information about a gamification level milestone")
public record LevelResponse(
        @Schema(description = "Level sequence number (1-based)", example = "1")
        Short levelNo,

        @Schema(description = "Total XP threshold required to reach this level", example = "0")
        Integer requiredXp,

        @Schema(description = "Display title or rank of this level", example = "Novice")
        String title
) {
}
