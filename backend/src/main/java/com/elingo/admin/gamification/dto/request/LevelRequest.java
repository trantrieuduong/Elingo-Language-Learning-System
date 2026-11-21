package com.elingo.admin.gamification.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record LevelRequest(
        @NotNull(message = "INVALID_REQUEST")
        @PositiveOrZero(message = "LEVEL_REQUIRED_XP_INVALID")
        Integer requiredXp,

        @Size(max = 100, message = "INVALID_REQUEST")
        String title
) {
    public LevelRequest {
        title = (title == null || title.isBlank()) ? null : title.strip();
    }
}
