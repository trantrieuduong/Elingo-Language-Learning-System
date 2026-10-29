package com.elingo.battle.service.impl;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.response.MatchFoundResponse;
import com.elingo.battle.dto.response.CreatedMatchResult;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.service.BattleMatchPersistenceService;
import com.elingo.battle.service.BattleMatchService;
import com.elingo.battle.state.LiveMatch;
import com.elingo.battle.websocket.BattleEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j(topic = "BATTLE-SERVICE")
@Service
@RequiredArgsConstructor
public class BattleMatchServiceImpl implements BattleMatchService {

    private final BattleMatchPersistenceService persistenceService;
    private final BattleEventPublisher eventPublisher;
    private final BattleProperties battleProperties;

    @Qualifier("battleScheduler")
    private final ThreadPoolTaskScheduler battleScheduler;

    // Trận đang có trong RAM: matchId -> LiveMatch
    private final Map<Long, LiveMatch> liveMatches = new ConcurrentHashMap<>();

    // userId -> matchId (để kiểm tra user đang trong trận nào)
    private final Map<Long, Long> userActiveMatch = new ConcurrentHashMap<>();

    @Override
    public void createRandomMatch(Long userAId, Long userBId, BattleMode mode) {
        // 1. Lưu DB qua bean riêng
        CreatedMatchResult data = persistenceService.persistNewRandomMatch(userAId, userBId, mode);

        // 2. Đưa vào RAM
        LiveMatch liveMatch = new LiveMatch(data.matchId(), mode, userAId, userBId);
        liveMatches.put(data.matchId(), liveMatch);
        userActiveMatch.put(userAId, data.matchId());
        userActiveMatch.put(userBId, data.matchId());

        // 3. Gửi match:found cho cả hai
        MatchFoundResponse foundResponse = new MatchFoundResponse(
                data.matchId(),
                mode,
                BattleMatchType.RANDOM,
                battleProperties.totalRounds(),
                battleProperties.countdownMs(),
                data.players()
        );
        eventPublisher.sendToUser(userAId, "match:found", foundResponse);
        eventPublisher.sendToUser(userBId, "match:found", foundResponse);

        ScheduledFuture<?> future = battleScheduler.schedule(
                () -> cancelMatchStub(data.matchId(), userAId, userBId),
                Instant.now().plusMillis(battleProperties.countdownMs())
        );
        liveMatch.setPendingFuture(future);
    }

    @Override
    public boolean isUserInActiveMatch(Long userId) {
        Long matchId = userActiveMatch.get(userId);
        if (matchId == null) return false;
        LiveMatch live = liveMatches.get(matchId);
        return live != null && live.getStatus() == BattleMatchStatus.IN_PROGRESS;
    }

    private void cancelMatchStub(Long matchId, Long userAId, Long userBId) {
        LiveMatch live = liveMatches.get(matchId);
        if (live == null) return;
        live.lock();
        try {
            if (live.getStatus() != BattleMatchStatus.IN_PROGRESS) return;
            live.setStatus(BattleMatchStatus.CANCELLED);
            persistenceService.updateMatchStatus(matchId, BattleMatchStatus.CANCELLED);
            cleanupMatchState(matchId, userAId, userBId);
            log.info("Match stub cancelled matchId={} — TODO BE2: replace with game loop", matchId);
        } finally {
            live.unlock();
        }
    }

    private void cleanupMatchState(Long matchId, Long userAId, Long userBId) {
        liveMatches.remove(matchId);
        userActiveMatch.remove(userAId);
        userActiveMatch.remove(userBId);
    }
}
