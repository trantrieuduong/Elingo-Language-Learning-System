package com.elingo.battle.dto.response;

import com.elingo.battle.entity.BattleMode;

import java.util.List;

/**
 * Kết quả trả về từ BattleMatchPersistenceService.persistNewRandomMatch
 * sau khi transaction commit — dùng để đưa vào RAM và gửi event.
 */
public record CreatedMatchResult(
        Long matchId,
        BattleMode mode,
        Long userAId,
        Long userBId,
        List<PlayerResponse> players
) {
}
