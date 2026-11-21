package com.elingo.admin.gamification.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;

import java.time.LocalDateTime;

public record AdminBadgeResponse(
        Long id,
        String code,
        String name,
        String description,
        @BuildMediaUrl
        String iconUrl,
        BadgeConditionType conditionType,
        Integer conditionValue,
        LocalDateTime createdAt
) {
    public static AdminBadgeResponse from(Badge entity) {
        if (entity == null) return null;
        return new AdminBadgeResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getIconUrl(),
                entity.getConditionType(),
                entity.getConditionValue(),
                entity.getCreatedAt()
        );
    }
}
