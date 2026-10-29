package com.elingo.battle.service;

import com.elingo.battle.dto.response.CreatedMatchResult;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMode;

public interface BattleMatchPersistenceService {

    /**
     * Lưu BattleMatch + 2 BattleParticipant trong một transaction.
     * Trả về dữ liệu cần dùng sau commit (không gửi event bên trong).
     */
    CreatedMatchResult persistNewRandomMatch(Long userAId, Long userBId, BattleMode mode);

    void updateMatchStatus(Long matchId, BattleMatchStatus status);
}
