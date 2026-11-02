package com.elingo.learning.dictation.service.impl;

import com.elingo.learning.dictation.service.DictationScoringService;
import com.elingo.learning.lesson.config.LessonProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DictationScoringServiceImpl implements DictationScoringService {

    private final LessonProperties lessonProperties;

    @Override
    public int calculateScore(int attemptCount, int hintUsedCount, int totalWords) {
        // score = max(0, round(100 - (attemptCount - 1) * attemptPenalty - hintPenalty))
        // hintPenalty = min(hintUsedCount, totalWords) / totalWords * 100
        double hintPenalty = totalWords <= 0 ? 0.0 : (double) Math.min(hintUsedCount, totalWords) / totalWords * 100.0;
        int attemptPenalty = lessonProperties.dictationAttemptPenalty();

        long rawScore = Math.round(100.0 - (attemptCount - 1) * attemptPenalty - hintPenalty);
        return (int) Math.max(0, Math.min(100, rawScore));
    }
}
