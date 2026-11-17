package com.elingo.gamification.service;

import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;

import java.util.List;

public interface BadgeAwardService {
    List<Badge> evaluateAndAward(Long userId, BadgeConditionType conditionType);
}
