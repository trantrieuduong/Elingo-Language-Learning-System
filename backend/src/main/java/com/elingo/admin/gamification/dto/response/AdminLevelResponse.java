package com.elingo.admin.gamification.dto.response;

import com.elingo.gamification.entity.Level;

import java.time.LocalDateTime;

public record AdminLevelResponse(
        Short levelNo,
        Integer requiredXp,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminLevelResponse from(Level entity) {
        if (entity == null) return null;
        return new AdminLevelResponse(
                entity.getLevelNo(),
                entity.getRequiredXp(),
                entity.getTitle(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
