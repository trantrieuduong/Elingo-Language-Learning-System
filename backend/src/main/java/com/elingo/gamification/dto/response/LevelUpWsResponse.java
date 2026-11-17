package com.elingo.gamification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "WebSocket payload pushed to user when level up occurs")
public record LevelUpWsResponse(
        @Schema(description = "New level attained", example = "2")
        int newLevel,

        @Schema(description = "Title of new level", example = "Explorer")
        String title,

        @Schema(description = "Timestamp when level up occurred", example = "2026-11-16T19:30:00")
        LocalDateTime unlockedAt
) {
}
