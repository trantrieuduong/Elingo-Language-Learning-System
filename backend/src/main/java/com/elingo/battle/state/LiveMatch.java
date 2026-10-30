package com.elingo.battle.state;

import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
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
    private final int totalRounds;
    private final Long userAId;
    private final Long userBId;
    private final Map<Long, Long> participantIds;       
    private final List<LiveQuestion> questions;          
    private final ReentrantLock lock = new ReentrantLock();

    @Setter private volatile BattleMatchStatus status = BattleMatchStatus.IN_PROGRESS;
    @Setter private volatile ScheduledFuture<?> pendingFuture;   // timer đang chờ

    public LiveMatch(Long matchId, BattleMode mode, int totalRounds,
                     Long userAId, Long userBId,
                     Map<Long, Long> participantIds, List<LiveQuestion> questions) {
        this.matchId = matchId;
        this.mode = mode;
        this.totalRounds = totalRounds;
        this.userAId = userAId;
        this.userBId = userBId;
        this.participantIds = participantIds;
        this.questions = questions;
    }

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

    public Long getParticipantId(Long userId) {
        return participantIds.get(userId);
    }
}
