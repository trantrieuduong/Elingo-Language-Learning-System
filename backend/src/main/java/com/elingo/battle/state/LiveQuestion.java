package com.elingo.battle.state;

import java.util.List;

/** Snapshot câu hỏi giữ trong RAM */
public record LiveQuestion(
        Long questionId,
        int roundNo,
        String term,
        String correctAnswer,
        int timeLimitMs,
        List<LiveOption> options
) {
    public record LiveOption(Long id, String text, boolean correct) {}

    public boolean hasOption(Long optionId) {
        return options.stream().anyMatch(o -> o.id().equals(optionId));
    }

    public boolean isCorrectOption(Long optionId) {
        return options.stream().anyMatch(o -> o.id().equals(optionId) && o.correct());
    }
}

