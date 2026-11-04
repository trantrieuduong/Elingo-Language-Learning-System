package com.elingo.battle.state;

import com.elingo.battle.entity.BattleMatchStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nơi giữ trạng thái trận đang có trong RAM.
 */
@Component
public class BattleLiveRegistry {

    private final Map<Long, LiveMatch> matches = new ConcurrentHashMap<>();  // matchId -> match
    private final Map<Long, Long> userMatch = new ConcurrentHashMap<>();  // userId  -> matchId

    public void register(LiveMatch m) {
        matches.put(m.getMatchId(), m);
        userMatch.put(m.getUserAId(), m.getMatchId());
        userMatch.put(m.getUserBId(), m.getMatchId());
    }

    public LiveMatch get(Long matchId) {
        return matches.get(matchId);
    }

    public LiveMatch getByUser(Long userId) {
        Long id = userMatch.get(userId);
        return id == null ? null : matches.get(id);
    }

    public boolean isUserInActiveMatch(Long userId) {
        LiveMatch m = getByUser(userId);
        return m != null && m.getStatus() == BattleMatchStatus.IN_PROGRESS;
    }

    // Xóa trận khỏi registry
    public void remove(LiveMatch m) {
        matches.remove(m.getMatchId());
        userMatch.remove(m.getUserAId(), m.getMatchId());
        userMatch.remove(m.getUserBId(), m.getMatchId());
    }
}

