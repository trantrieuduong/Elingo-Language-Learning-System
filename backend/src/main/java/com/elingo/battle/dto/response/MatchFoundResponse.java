package com.elingo.battle.dto.response;

import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;

import java.util.List;

public record MatchFoundResponse(
        Long matchId,
        BattleMode mode,
        BattleMatchType matchType,
        int totalRounds,
        long countdownMs,
        List<PlayerResponse> players
) {
}
