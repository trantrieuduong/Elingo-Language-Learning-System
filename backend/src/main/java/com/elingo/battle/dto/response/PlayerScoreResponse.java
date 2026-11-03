package com.elingo.battle.dto.response;

public record PlayerScoreResponse(
        Long userId,
        int score,
        int correctCount
) {
}

