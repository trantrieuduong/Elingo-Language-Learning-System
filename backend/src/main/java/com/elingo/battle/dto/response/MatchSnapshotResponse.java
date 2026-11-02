package com.elingo.battle.dto.response;

import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;

import java.util.List;

public record MatchSnapshotResponse(
        Long matchId,
        BattleMode mode,
        BattleMatchType matchType,
        int totalRounds,
        List<PlayerResponse> players,
        List<PlayerScoreResponse> scores,
        int roundNo,
        MatchQuestionResponse question,
        boolean answered,
        RoundResultResponse lastRoundResult
) {
}

