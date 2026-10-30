package com.elingo.battle.state;

public record LiveAnswer(
        Long userId,
        Long selectedOptionId,
        String typedAnswer,
        boolean correct,
        int responseTimeMs,
        int points
) {
}

