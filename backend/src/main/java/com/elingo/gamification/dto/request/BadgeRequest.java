package com.elingo.gamification.dto.request;

import com.elingo.common.annotation.AllowedMediaKind;
import com.elingo.common.annotation.BuildMediaKey;
import com.elingo.common.enums.MediaKind;
import com.elingo.gamification.entity.BadgeConditionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Locale;

@Schema(description = "Request payload for creating or updating a badge")
public record BadgeRequest(
        @Schema(
                description = "Unique alphanumeric code of the badge (converted to lowercase)",
                example = "streak-7-days",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 100, message = "INVALID_REQUEST")
        String code,

        @Schema(
                description = "Display name of the badge",
                example = "7-Day Streak Master",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 150, message = "INVALID_REQUEST")
        String name,

        @Schema(
                description = "Detailed description explaining how to earn this badge",
                example = "Maintain a learning streak for 7 consecutive days"
        )
        @Size(max = 500, message = "INVALID_REQUEST")
        String description,

        @Schema(
                description = "Media storage key or path for the badge icon (supports IMAGE kind)",
                example = "gamification/badges/streak-7-days.png"
        )
        @Size(max = 500, message = "INVALID_REQUEST")
        @AllowedMediaKind(MediaKind.IMAGE)
        @BuildMediaKey
        String iconUrl,

        @Schema(
                description = "Condition metric required to unlock this badge",
                example = "STREAK_DAYS",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "INVALID_REQUEST")
        BadgeConditionType conditionType,

        @Schema(
                description = "Threshold value required to satisfy the condition (must be positive)",
                example = "7",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Integer conditionValue
) {
    public BadgeRequest {
        code = code == null ? null : code.strip().toLowerCase(Locale.ROOT);
        name = name == null ? null : name.strip();
        description = (description == null || description.isBlank()) ? null : description.strip();
        iconUrl = (iconUrl == null || iconUrl.isBlank()) ? null : iconUrl.strip();
    }
}
