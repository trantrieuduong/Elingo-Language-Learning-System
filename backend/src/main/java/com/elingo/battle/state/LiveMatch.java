package com.elingo.battle.state;

import com.elingo.battle.dto.response.PlayerResponse;
import com.elingo.battle.dto.response.RoundResultResponse;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * POJO lưu trong RAM, đại diện một trận đang diễn ra.
 * Mọi thay đổi state phải đi qua lock() / unlock().
 */
@Getter
public class LiveMatch {

    private final Long matchId;
    private final BattleMode mode;
    private final BattleMatchType matchType;
    private final int totalRounds;
    private final Long userAId;
    private final Long userBId;
    private final Map<Long, Long> participantIds;
    private final List<LiveQuestion> questions;
    private final ReentrantLock lock = new ReentrantLock();

    // Điểm số tích lũy - truy cập chỉ trong lock
    private final Map<Long, Integer> scores = new HashMap<>();
    private final Map<Long, Integer> correctCounts = new HashMap<>();

    // Mất kết nối & thời gian chờ (grace period)
    private final Set<Long> disconnected = new HashSet<>();
    private final Map<Long, ScheduledFuture<?>> graceFutures = new HashMap<>();

    @Setter private volatile BattleMatchStatus status = BattleMatchStatus.IN_PROGRESS;
    @Setter private volatile ScheduledFuture<?> pendingFuture;
    @Setter private LiveRound currentRound;

    @Setter private volatile List<PlayerResponse> players;
    @Setter private RoundResultResponse lastRoundResult;

    public LiveMatch(Long matchId, BattleMode mode, BattleMatchType matchType, int totalRounds,
                     Long userAId, Long userBId,
                     Map<Long, Long> participantIds, List<LiveQuestion> questions) {
        this.matchId = matchId;
        this.mode = mode;
        this.matchType = matchType;
        this.totalRounds = totalRounds;
        this.userAId = userAId;
        this.userBId = userBId;
        this.participantIds = participantIds;
        this.questions = questions;
        scores.put(userAId, 0);
        scores.put(userBId, 0);
        correctCounts.put(userAId, 0);
        correctCounts.put(userBId, 0);
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public boolean hasUser(Long userId) {
        return userAId.equals(userId) || userBId.equals(userId);
    }

    public Long opponentOf(Long userId) {
        if (userAId.equals(userId)) return userBId;
        if (userBId.equals(userId)) return userAId;
        return null;
    }

    public Long getParticipantId(Long userId) {
        return participantIds.get(userId);
    }

    // Cộng điểm cho userId (gọi trong lock)
    public void addScore(Long userId, int points, boolean correct) {
        scores.merge(userId, points, Integer::sum);
        if (correct) correctCounts.merge(userId, 1, Integer::sum);
    }

    public int getScore(Long userId) {
        return scores.getOrDefault(userId, 0);
    }

    public int getCorrectCount(Long userId) {
        return correctCounts.getOrDefault(userId, 0);
    }

    public boolean markDisconnected(Long userId) {
        return disconnected.add(userId);
    }

    public boolean isDisconnected(Long userId) {
        return disconnected.contains(userId);
    }

    public void putGrace(Long userId, ScheduledFuture<?> f) {
        cancelGrace(userId);
        graceFutures.put(userId, f);
    }

    public void cancelGrace(Long userId) {
        ScheduledFuture<?> f = graceFutures.remove(userId);
        if (f != null) f.cancel(false);
    }

    public void cancelAllGraces() {
        graceFutures.values().forEach(f -> f.cancel(false));
        graceFutures.clear();
    }

    public boolean markConnected(Long userId) {
        cancelGrace(userId);
        return disconnected.remove(userId);
    }
}
