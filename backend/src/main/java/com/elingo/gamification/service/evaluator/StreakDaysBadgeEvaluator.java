package com.elingo.gamification.service.evaluator;

import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.repository.LearningStreakRepository;
import com.elingo.gamification.service.BadgeEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StreakDaysBadgeEvaluator implements BadgeEvaluator {

    private final LearningStreakRepository streakRepo;

    @Override
    public BadgeConditionType supportsType() {
        return BadgeConditionType.STREAK_DAYS;
    }

    @Override
    public long getCurrentProgress(long userId) {
        return streakRepo.findById(userId)
                .map(s -> (long) s.getLongestStreak())
                .orElse(0L);
    }
}
