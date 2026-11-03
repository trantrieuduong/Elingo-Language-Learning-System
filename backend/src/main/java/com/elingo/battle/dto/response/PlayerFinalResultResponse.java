package com.elingo.battle.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlayerFinalResultResponse(
        Long userId,
        int score,
        int correctCount,
        @JsonProperty("isWinner") boolean isWinner
) {
}

