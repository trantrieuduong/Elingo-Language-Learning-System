package com.elingo.gamification.service.evaluator;

import com.elingo.battle.repository.BattleParticipantRepository;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.service.BadgeEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BattleWinsBadgeEvaluator implements BadgeEvaluator {

    private final BattleParticipantRepository battleParticipantRepository;

    @Override
    public BadgeConditionType supportsType() {
        return BadgeConditionType.BATTLE_WINS;
    }

    @Override
    public long getCurrentProgress(long userId) {
        var stats = battleParticipantRepository.getStats(userId);
        return stats != null ? stats.getWins() : 0L;
    }
}
