package com.elingo.battle.service.impl;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.service.BattleXpService;
import com.elingo.battle.state.LiveMatch;
import com.elingo.common.event.XpGainedEvent;
import com.elingo.gamification.entity.XpSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j(topic = "BATTLE-XP-SERVICE")
@Service
@RequiredArgsConstructor
public class BattleXpServiceImpl implements BattleXpService {

    private final BattleProperties props;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void awardMatchXp(LiveMatch m, Long winnerId, String reason) {
        if (m.getMatchType() != BattleMatchType.RANDOM) return;

        // Xác định người bị xử thua khi trận kết thúc do FORFEIT
        Long forfeitUserId = ("FORFEIT".equals(reason) && winnerId != null) ? m.opponentOf(winnerId) : null;

        for (Long userId : List.of(m.getUserAId(), m.getUserBId())) {
            if (userId.equals(forfeitUserId)) continue;
            if (m.getCorrectCount(userId) < props.minCorrectForXp()) continue;

            // Hòa (winnerId = null) -> cả hai nhận BATTLE_LOSE
            XpSource source = userId.equals(winnerId) ? XpSource.BATTLE_WIN : XpSource.BATTLE_LOSE;
            eventPublisher.publishEvent(new XpGainedEvent(userId, m.getMatchId(), source, LocalDateTime.now()));
            log.info("Battle XP event published matchId={} userId={} source={}", m.getMatchId(), userId, source);
        }
    }
}

