package com.elingo.battle.dto.response;

import com.elingo.battle.entity.BattleMode;

import java.util.List;

public record MatchQuestionResponse(
        Long matchId,
        Long questionId,
        int roundNo,
        int totalRounds,
        String term,
        List<OptionResponse> options,
        BattleMode mode,
        int timeLimitMs,
        long serverNow,
        long deadlineAt
) {
}

