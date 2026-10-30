package com.elingo.battle.service.impl;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.response.MatchFoundResponse;
import com.elingo.battle.dto.response.MatchQuestionResponse;
import com.elingo.battle.dto.response.OptionResponse;
import com.elingo.battle.dto.response.CreatedMatchResult;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.service.BattleMatchPersistenceService;
import com.elingo.battle.service.BattleMatchService;
import com.elingo.battle.state.BattleLiveRegistry;
import com.elingo.battle.state.LiveMatch;
import com.elingo.battle.state.LiveQuestion;
import com.elingo.battle.websocket.BattleEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j(topic = "BATTLE-SERVICE")
@Service
@RequiredArgsConstructor
public class BattleMatchServiceImpl implements BattleMatchService {

    private final BattleMatchPersistenceService persistenceService;
    private final BattleEventPublisher eventPublisher;
    private final BattleProperties battleProperties;
    private final BattleLiveRegistry registry;

    @Qualifier("battleScheduler")
    private final ThreadPoolTaskScheduler battleScheduler;

    @Override
    public void createRandomMatch(Long userAId, Long userBId, BattleMode mode) {
        // 1. Lưu DB + sinh câu hỏi
        CreatedMatchResult data = persistenceService.persistNewRandomMatch(userAId, userBId, mode);

        // 2. Đưa vào RAM registry
        LiveMatch live = new LiveMatch(data.matchId(), mode, battleProperties.totalRounds(),
                userAId, userBId, data.participantIds(), data.questions());
        registry.register(live);

        // 3. Gửi match:found cho cả hai
        MatchFoundResponse found = new MatchFoundResponse(
                data.matchId(), mode, BattleMatchType.RANDOM,
                battleProperties.totalRounds(), battleProperties.countdownMs(), data.players());
        eventPublisher.sendToUser(userAId, "match:found", found);
        eventPublisher.sendToUser(userBId, "match:found", found);

        // 4. Đặt timer gửi câu 1 sau countdown
        live.setPendingFuture(battleScheduler.schedule(
                () -> sendFirstQuestionStub(live),
                Instant.now().plusMillis(battleProperties.countdownMs())));
    }

    @Override
    public boolean isUserInActiveMatch(Long userId) {
        return registry.isUserInActiveMatch(userId);
    }

    private void sendFirstQuestionStub(LiveMatch m) {
        m.lock();
        try {
            if (m.getStatus() != BattleMatchStatus.IN_PROGRESS)
                return;
            LiveQuestion q = m.getQuestions().get(0);
            long now = System.currentTimeMillis();
            MatchQuestionResponse payload = new MatchQuestionResponse(
                    m.getMatchId(), q.questionId(), 1, m.getTotalRounds(), q.term(),
                    q.options().stream()
                            .map(o -> new OptionResponse(o.id(), o.text())) // Không kèm correct
                            .toList(),
                    m.getMode(), q.timeLimitMs(), now, now + q.timeLimitMs());
            eventPublisher.sendToUser(m.getUserAId(), "match:question", payload);
            eventPublisher.sendToUser(m.getUserBId(), "match:question", payload);
            m.setPendingFuture(battleScheduler.schedule(
                    () -> cancelStub(m),
                    Instant.now().plusMillis(q.timeLimitMs())));
        } finally {
            m.unlock();
        }
    }

    private void cancelStub(LiveMatch m) {
        m.lock();
        try {
            if (m.getStatus() != BattleMatchStatus.IN_PROGRESS) return;
            m.setStatus(BattleMatchStatus.CANCELLED);
            persistenceService.updateMatchStatus(m.getMatchId(), BattleMatchStatus.CANCELLED);
            registry.remove(m);
            log.info("Match stub cancelled matchId={}", m.getMatchId());
        } finally {
            m.unlock();
        }
    }
}
