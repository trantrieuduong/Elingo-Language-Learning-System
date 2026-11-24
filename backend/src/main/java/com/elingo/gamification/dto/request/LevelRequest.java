package com.elingo.gamification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for creating or updating a level milestone")
public record LevelRequest(
        @Schema(
                description = "Cumulative XP required to reach this level (must be >= 0, and strictly higher than preceding level)",
                example = "100",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "INVALID_REQUEST")
        @PositiveOrZero(message = "LEVEL_REQUIRED_XP_INVALID")
        Integer requiredXp,

        @Schema(
                description = "Optional honorary title for this level milestone",
                example = "Novice Explorer"
        )
        @Size(max = 100, message = "INVALID_REQUEST")
        String title
) {
    public LevelRequest {
        title = (title == null || title.isBlank()) ? null : title.strip();
    }
}
