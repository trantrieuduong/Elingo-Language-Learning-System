package com.elingo.battle.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlayerRoundResultResponse(
        Long userId,
        boolean answered,
        @JsonProperty("isCorrect") boolean isCorrect,
        int pointsGained,
        int score,
        int correctCount,
        int responseTimeMs
) {
}

