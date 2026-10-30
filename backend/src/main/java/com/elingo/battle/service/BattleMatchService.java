package com.elingo.battle.service;

import com.elingo.battle.entity.BattleMode;

public interface BattleMatchService {
    void createRandomMatch(Long userAId, Long userBId, BattleMode mode);
    boolean isUserInActiveMatch(Long userId);
}
