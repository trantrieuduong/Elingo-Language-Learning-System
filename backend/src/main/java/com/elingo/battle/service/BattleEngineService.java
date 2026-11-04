package com.elingo.battle.service;

import com.elingo.battle.dto.request.BattleAnswerRequest;
import com.elingo.battle.dto.response.MatchRejoinedResponse;
import com.elingo.battle.state.LiveMatch;

public interface BattleEngineService {
    void start(LiveMatch match);
    void submitAnswer(Long userId, BattleAnswerRequest request);
    void forfeit(LiveMatch match, Long loserId);
    void cancel(LiveMatch match, String reason);
    MatchRejoinedResponse snapshot(LiveMatch match, Long userId);
}

