package com.elingo.learning.shadowing.service.impl;

import com.elingo.learning.lesson.config.LessonProperties;
import com.elingo.learning.shadowing.service.ShadowingScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShadowingScoringServiceImpl implements ShadowingScoringService {

    private final LessonProperties lessonProperties;

    @Override
    public int calculateFinalScore(int score, int attemptCount) {
        // finalScore = max(0, score - (attemptCount - 1) * penalty)
        int penalty = lessonProperties.shadowingAttemptPenalty();
        return Math.max(0, score - (attemptCount - 1) * penalty);
    }
}
