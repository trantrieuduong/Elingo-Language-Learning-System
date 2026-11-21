package com.elingo.admin.gamification.dto.request;

import com.elingo.common.annotation.AllowedMediaKind;
import com.elingo.common.annotation.BuildMediaKey;
import com.elingo.common.enums.MediaKind;
import com.elingo.gamification.entity.BadgeConditionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BadgeRequest(
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 100, message = "INVALID_REQUEST")
        String code,

        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 150, message = "INVALID_REQUEST")
        String name,

        @Size(max = 500, message = "INVALID_REQUEST")
        String description,

        @Size(max = 500, message = "INVALID_REQUEST")
        @AllowedMediaKind(MediaKind.IMAGE)
        @BuildMediaKey
        String iconUrl,

        @NotNull(message = "INVALID_REQUEST")
        BadgeConditionType conditionType,

        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Integer conditionValue
) {
    public BadgeRequest {
        code = code == null ? null : code.strip().toUpperCase();
        name = name == null ? null : name.strip();
        description = (description == null || description.isBlank()) ? null : description.strip();
        iconUrl = (iconUrl == null || iconUrl.isBlank()) ? null : iconUrl.strip();
    }
}
