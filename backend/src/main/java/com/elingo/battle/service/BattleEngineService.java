package com.elingo.battle.service;

import com.elingo.battle.dto.request.BattleAnswerRequest;
import com.elingo.battle.state.LiveMatch;

public interface BattleEngineService {
    void start(LiveMatch match);
    void submitAnswer(Long userId, BattleAnswerRequest request);
}

