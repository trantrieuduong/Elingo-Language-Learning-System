package com.elingo.battle.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BattleRoundAnswerResponse(
        Long userId,
        boolean answered,
        String answerText,
        @JsonProperty("isCorrect") boolean isCorrect,
        Integer responseTimeMs
) {
}

