package com.elingo.battle.dto.request;

import jakarta.validation.constraints.NotNull;

public record BattleAnswerRequest(
        @NotNull(message = "BATTLE_MATCH_NOT_FOUND") 
        Long matchId,
        @NotNull(message = "BATTLE_QUESTION_NOT_FOUND") 
        Long questionId,
        Long selectedOptionId,
        String typedAnswer
) {
}

