package com.elingo.gamification.service;

import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;

public interface BadgeEvaluator {
    BadgeConditionType supportsType();

    long getCurrentProgress(long userId);

    default boolean isEligible(long currentProgress, Badge badge) {
        return currentProgress >= badge.getConditionValue();
    }
}
