package com.elingo.battle.dto.response;

import com.elingo.battle.entity.BattleMode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
        long deadlineAt,
        Integer answerLength,             
        List<Integer> answerPattern,       
        String firstChar                 
) {
}
