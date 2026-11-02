package com.elingo.battle.service.impl;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.response.ActiveMatchResponse;
import com.elingo.battle.dto.response.OpponentDisconnectedResponse;
import com.elingo.battle.dto.response.OpponentReconnectedResponse;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.service.BattleConnectionService;
import com.elingo.battle.service.BattleEngineService;
import com.elingo.battle.state.BattleLiveRegistry;
import com.elingo.battle.state.LiveMatch;
import com.elingo.battle.websocket.BattleEventPublisher;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j(topic = "BATTLE-CONNECTION-SERVICE")
@Service
@RequiredArgsConstructor
public class BattleConnectionServiceImpl implements BattleConnectionService {

    private final BattleLiveRegistry registry;
    private final BattleEngineService engineService;
    private final BattleEventPublisher publisher;
    private final BattleProperties props;

    @Qualifier("battleScheduler")
    private final ThreadPoolTaskScheduler battleScheduler;

    @Override
    public void onUserOffline(Long userId) {
        LiveMatch m = registry.getByUser(userId);
        if (m == null) return;
        m.lock();
        try {
            if (m.getStatus() != BattleMatchStatus.IN_PROGRESS) return;
            if (!m.markDisconnected(userId)) return; // đã ngắt rồi (tránh lặp)
            publisher.sendToUser(m.opponentOf(userId), "match:opponent-disconnected",
                    new OpponentDisconnectedResponse(m.getMatchId(), props.reconnectGraceMs()));
            m.putGrace(userId, battleScheduler.schedule(
                    () -> onGraceExpired(m, userId),
                    Instant.now().plusMillis(props.reconnectGraceMs())));
            log.info("Player disconnected matchId={} userId={} graceMs={}",
                    m.getMatchId(), userId, props.reconnectGraceMs());
        } finally {
            m.unlock();
        }
    }

    private void onGraceExpired(LiveMatch m, Long userId) {
        try {
            m.lock();
            try {
                if (m.getStatus() != BattleMatchStatus.IN_PROGRESS || !m.isDisconnected(userId)) return;
                if (m.isDisconnected(m.opponentOf(userId))) {
                    engineService.cancel(m, "BOTH_DISCONNECTED");
                } else {
                    engineService.forfeit(m, userId);
                }
            } finally {
                m.unlock();
            }
        } catch (Exception ex) {
            log.error("Forfeit failed matchId={} userId={}", m.getMatchId(), userId, ex);
            engineService.cancel(m, "INTERNAL_ERROR");
        }
    }

    @Override
    public void rejoin(Long userId, Long matchId) {
        LiveMatch m = registry.getByUser(userId);
        if (m == null || !m.getMatchId().equals(matchId)) {
            throw new AppException(AppError.BATTLE_MATCH_NOT_FOUND);
        }
        m.lock();
        try {
            if (m.getStatus() != BattleMatchStatus.IN_PROGRESS) {
                throw new AppException(AppError.BATTLE_MATCH_NOT_FOUND);
            }
            boolean wasDisconnected = m.markConnected(userId); // hủy forfeit nếu đang đếm
            publisher.sendToUser(userId, "match:rejoined", engineService.snapshot(m, userId));
            if (wasDisconnected) {
                publisher.sendToUser(m.opponentOf(userId), "match:opponent-reconnected",
                        new OpponentReconnectedResponse(matchId));
            }
            log.info("Player rejoined matchId={} userId={} wasDisconnected={}", matchId, userId, wasDisconnected);
        } finally {
            m.unlock();
        }
    }

    @Override
    public void sendActiveMatch(Long userId) {
        LiveMatch m = registry.getByUser(userId);
        ActiveMatchResponse res = (m != null && m.getStatus() == BattleMatchStatus.IN_PROGRESS)
                ? new ActiveMatchResponse(m.getMatchId(), "IN_PROGRESS")
                : new ActiveMatchResponse(null, "NONE");
        publisher.sendToUser(userId, "active-match", res);
    }
}

