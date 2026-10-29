package com.elingo.battle.state;

import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.entity.BattleMatchStatus;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * POJO lưu trong RAM, đại diện một trận đang diễn ra.
 * Mọi thay đổi phải đi qua lock() / unlock().
 */
public class LiveMatch {

    private final Long matchId;
    private final BattleMode mode;
    private final Long userAId;
    private final Long userBId;
    private volatile BattleMatchStatus status;
    private final ReentrantLock lock = new ReentrantLock();

    // Scheduler future của countdown / round hiện tại — hủy khi không cần
    private volatile ScheduledFuture<?> pendingFuture;

    public LiveMatch(Long matchId, BattleMode mode, Long userAId, Long userBId) {
        this.matchId = matchId;
        this.mode = mode;
        this.userAId = userAId;
        this.userBId = userBId;
        this.status = BattleMatchStatus.IN_PROGRESS;
    }

    public Long getMatchId() { return matchId; }
    public BattleMode getMode() { return mode; }
    public Long getUserAId() { return userAId; }
    public Long getUserBId() { return userBId; }
    public BattleMatchStatus getStatus() { return status; }
    public void setStatus(BattleMatchStatus status) { this.status = status; }
    public ScheduledFuture<?> getPendingFuture() { return pendingFuture; }
    public void setPendingFuture(ScheduledFuture<?> pendingFuture) { this.pendingFuture = pendingFuture; }

    public void lock() { lock.lock(); }
    public void unlock() { lock.unlock(); }

    public boolean hasUser(Long userId) {
        return userAId.equals(userId) || userBId.equals(userId);
    }

    public Long opponentOf(Long userId) {
        if (userAId.equals(userId)) return userBId;
        if (userBId.equals(userId)) return userAId;
        return null;
    }
}
