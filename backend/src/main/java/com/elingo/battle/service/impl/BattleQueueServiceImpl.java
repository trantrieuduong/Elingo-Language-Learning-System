package com.elingo.battle.service.impl;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.response.QueueJoinedResponse;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.service.BattleMatchService;
import com.elingo.battle.service.BattleQueueService;
import com.elingo.battle.state.QueueEntry;
import com.elingo.battle.websocket.BattleEventPublisher;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j(topic = "BATTLE-SERVICE")
@Service
@RequiredArgsConstructor
public class BattleQueueServiceImpl implements BattleQueueService {

    private final BattleEventPublisher eventPublisher;
    private final BattleMatchService battleMatchService;
    private final BattleProperties battleProperties;

    @Qualifier("battleScheduler")
    private final ThreadPoolTaskScheduler battleScheduler;

    // Hàng đợi FIFO theo mode
    private final Map<BattleMode, Queue<QueueEntry>> queues = new ConcurrentHashMap<>();

    // userId -> timeout future
    private final Map<Long, ScheduledFuture<?>> timeoutFutures = new ConcurrentHashMap<>();

    // userId -> mode đang chờ
    private final Map<Long, BattleMode> userQueueMode = new ConcurrentHashMap<>();

    @Override
    public void join(Long userId, BattleMode mode) {
        // Kiểm tra đang trong trận IN_PROGRESS
        if (battleMatchService.isUserInActiveMatch(userId)) {
            throw new AppException(AppError.BATTLE_ALREADY_PARTICIPATED);
        }

        // Idempotent: đã trong hàng đợi → trả mode thật đang chờ, không đổi hàng đợi
        BattleMode currentMode = userQueueMode.get(userId);
        if (currentMode != null) {
            eventPublisher.sendToUser(userId, "queue:joined", new QueueJoinedResponse(currentMode));
            return;
        }

        // Lấy lock để tìm & gỡ đối thủ khỏi hàng đợi
        QueueEntry opponent;
        synchronized (this) {
            // Kiểm tra lại sau khi vào lock
            if (userQueueMode.containsKey(userId)) {
                BattleMode realMode = userQueueMode.get(userId);
                eventPublisher.sendToUser(userId, "queue:joined", new QueueJoinedResponse(realMode));
                return;
            }

            Queue<QueueEntry> queue = getQueue(mode);
            opponent = findOpponent(queue, userId);

            if (opponent != null) {
                // Gỡ đối thủ khỏi hàng đợi trong lock
                queue.remove(opponent);
                userQueueMode.remove(opponent.userId());
                cancelTimeout(opponent.userId());
            } else {
                queue.add(new QueueEntry(userId, mode, Instant.now()));
                userQueueMode.put(userId, mode);
                eventPublisher.sendToUser(userId, "queue:joined", new QueueJoinedResponse(mode));
                scheduleQueueTimeout(userId, mode);
            }
        }

        if (opponent != null) {
            log.info("Queue matched userId={} opponentId={} mode={}", userId, opponent.userId(), mode);
            tryCreateMatch(userId, opponent.userId(), mode);
        }
    }

    @Override
    public void leave(Long userId) {
        synchronized (this) {
            BattleMode mode = userQueueMode.remove(userId);
            if (mode == null) return; // không trong hàng đợi → bỏ qua
            getQueue(mode).removeIf(e -> e.userId().equals(userId));
            cancelTimeout(userId);
        }
        eventPublisher.sendToUser(userId, "queue:left", Collections.emptyMap());
    }

    @Override
    public void removeOnDisconnect(Long userId) {
        synchronized (this) {
            BattleMode mode = userQueueMode.remove(userId);
            if (mode != null) {
                getQueue(mode).removeIf(e -> e.userId().equals(userId));
                cancelTimeout(userId);
                log.info("User disconnected, removed from queue userId={} mode={}", userId, mode);
            }
        }
    }
    
    // Bọc createRandomMatch: nếu lỗi gửi error cho cả hai
    private void tryCreateMatch(Long userId, Long opponentId, BattleMode mode) {
        try {
            battleMatchService.createRandomMatch(userId, opponentId, mode);
        } catch (AppException ex) {
            log.warn("Match creation failed userId={} opponentId={} code={}", userId, opponentId, ex.getAppError().getCode());
            eventPublisher.sendError(userId, ex.getAppError().getCode(), ex.getMessage());
            eventPublisher.sendError(opponentId, ex.getAppError().getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error creating match userId={} opponentId={}", userId, opponentId, ex);
            eventPublisher.sendError(userId, "INTERNAL_ERROR", "Failed to create match");
            eventPublisher.sendError(opponentId, "INTERNAL_ERROR", "Failed to create match");
        }
    }

    private Queue<QueueEntry> getQueue(BattleMode mode) {
        return queues.computeIfAbsent(mode, m -> new LinkedList<>());
    }
    
    // Tìm người đầu tiên trong hàng đợi khác userId (tránh tự ghép với chính mình)
    private QueueEntry findOpponent(Queue<QueueEntry> queue, Long userId) {
        Iterator<QueueEntry> it = queue.iterator();
        while (it.hasNext()) {
            QueueEntry candidate = it.next();
            if (!candidate.userId().equals(userId)) {
                return candidate;
            }
        }
        return null;
    }

    private void scheduleQueueTimeout(Long userId, BattleMode mode) {
        ScheduledFuture<?> future = battleScheduler.schedule(
                () -> handleQueueTimeout(userId, mode),
                Instant.now().plusMillis(battleProperties.queueTimeoutMs())
        );
        timeoutFutures.put(userId, future);
    }

    private void handleQueueTimeout(Long userId, BattleMode mode) {
        synchronized (this) {
            // Luôn xóa timeoutFutures trước khi return
            timeoutFutures.remove(userId);
            if (!userQueueMode.containsKey(userId)) return; // đã rời hoặc đã ghép
            userQueueMode.remove(userId);
            getQueue(mode).removeIf(e -> e.userId().equals(userId));
        }
        eventPublisher.sendToUser(userId, "queue:timeout", Collections.emptyMap());
        log.info("Queue timeout userId={} mode={}", userId, mode);
    }

    private void cancelTimeout(Long userId) {
        ScheduledFuture<?> future = timeoutFutures.remove(userId);
        if (future != null) {
            future.cancel(false);
        }
    }
}
