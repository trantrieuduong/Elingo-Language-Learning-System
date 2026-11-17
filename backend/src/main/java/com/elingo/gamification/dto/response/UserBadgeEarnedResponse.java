package com.elingo.gamification.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.gamification.entity.BadgeConditionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Information of a badge earned by a user")
public record UserBadgeEarnedResponse(
        @Schema(description = "Unique ID of the badge", example = "1")
        Long id,

        @Schema(description = "Unique code of the badge", example = "STREAK_7_DAYS")
        String code,

        @Schema(description = "Display name of the badge", example = "7-Day Streak Master")
        String name,

        @Schema(description = "Description of the badge achievement", example = "Maintain a learning streak for 7 consecutive days")
        String description,

        @Schema(description = "URL to the icon image representing this badge", example = "https://cdn.elingo.com/badges/streak-7.png")
        @BuildMediaUrl String iconUrl,

        @Schema(description = "Type of condition required to earn this badge", example = "STREAK_DAYS")
        BadgeConditionType conditionType,

        @Schema(description = "Threshold value required to achieve this badge", example = "7")
        Integer conditionValue,

        @Schema(description = "Timestamp when the user earned this badge", example = "2026-11-16T10:30:00")
        LocalDateTime earnedAt
) {
}
