package com.elingo.gamification.dto.projection;

public interface ActivityByDateProjection {
    String getDateKey();
    Long getFlashcardCount();
    Long getDictationCount();
    Long getShadowingCount();
    Long getBattleCount();
    Long getCheckInCount();
    Long getTotalCount();
}
