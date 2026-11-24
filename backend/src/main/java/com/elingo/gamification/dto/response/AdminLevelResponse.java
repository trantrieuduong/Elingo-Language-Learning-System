package com.elingo.gamification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response payload containing level milestone details for administration")
public record AdminLevelResponse(
        @Schema(description = "Sequential level number (starts at 1)", example = "2")
        Short levelNo,

        @Schema(description = "Cumulative XP required to reach this level", example = "100")
        Integer requiredXp,

        @Schema(description = "Honorary title for this level milestone", example = "Novice Explorer")
        String title,

        @Schema(description = "Timestamp when the level was created", example = "2026-11-21T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Timestamp when the level was last updated", example = "2026-11-21T12:00:00")
        LocalDateTime updatedAt
) {
}
