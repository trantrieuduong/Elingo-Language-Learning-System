package com.elingo.battle.service;

import com.elingo.battle.state.LiveMatch;

public interface BattleXpService {
    void awardMatchXp(LiveMatch match, Long winnerId, String reason);
}

