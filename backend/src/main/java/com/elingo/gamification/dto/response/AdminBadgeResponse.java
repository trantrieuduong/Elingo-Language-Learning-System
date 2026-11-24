package com.elingo.gamification.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.gamification.entity.BadgeConditionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response payload containing detailed badge information for administration")
public record AdminBadgeResponse(
        @Schema(description = "Unique identifier of the badge", example = "1")
        Long id,

        @Schema(description = "Unique code of the badge", example = "streak-7-days")
        String code,

        @Schema(description = "Display name of the badge", example = "7-Day Streak Master")
        String name,

        @Schema(description = "Detailed description of the badge", example = "Maintain a learning streak for 7 consecutive days")
        String description,

        @Schema(description = "Public CDN URL of the badge icon", example = "https://cdn.elingo.com/gamification/badges/streak-7-days.png")
        @BuildMediaUrl
        String iconUrl,

        @Schema(description = "Condition metric required to unlock this badge", example = "STREAK_DAYS")
        BadgeConditionType conditionType,

        @Schema(description = "Numeric threshold to satisfy the condition", example = "7")
        Integer conditionValue,

        @Schema(description = "Timestamp when the badge was created", example = "2026-11-21T10:00:00")
        LocalDateTime createdAt
) {
}
