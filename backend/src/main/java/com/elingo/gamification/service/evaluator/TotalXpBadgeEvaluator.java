package com.elingo.gamification.service.evaluator;

import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.repository.UserXpRepository;
import com.elingo.gamification.service.BadgeEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TotalXpBadgeEvaluator implements BadgeEvaluator {

    private final UserXpRepository userXpRepo;

    @Override
    public BadgeConditionType supportsType() {
        return BadgeConditionType.TOTAL_XP;
    }

    @Override
    public long getCurrentProgress(long userId) {
        return userXpRepo.findById(userId)
                .map(u -> (long) u.getTotalXp())
                .orElse(0L);
    }
}
