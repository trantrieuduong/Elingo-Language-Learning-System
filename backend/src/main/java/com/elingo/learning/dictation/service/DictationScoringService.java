package com.elingo.learning.dictation.service;

public interface DictationScoringService {
    int calculateScore(int attemptCount, int hintUsedCount, int totalWords);
}
