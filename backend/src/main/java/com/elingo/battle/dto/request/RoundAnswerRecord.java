package com.elingo.battle.dto.request;

public record RoundAnswerRecord(
        Long questionId,
        Long participantId,
        Long selectedOptionId,
        String typedAnswer,
        boolean correct,
        int responseTimeMs
) {
}

