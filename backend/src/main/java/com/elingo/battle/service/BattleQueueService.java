package com.elingo.battle.service;

import com.elingo.battle.entity.BattleMode;

public interface BattleQueueService {

    void join(Long userId, BattleMode mode);

    void leave(Long userId);

    void removeOnDisconnect(Long userId);
}
