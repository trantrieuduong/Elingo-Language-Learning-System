package com.elingo.battle.dto.response;

import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;

import java.time.LocalDateTime;
import java.util.List;

public record BattleMatchDetailResponse(
        Long matchId,
        BattleMode mode,
        BattleMatchType matchType,
        int totalRounds,
        Long winnerId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<PlayerResponse> players,
        List<PlayerFinalResultResponse> results,
        List<BattleRoundDetailResponse> rounds
) {
}

