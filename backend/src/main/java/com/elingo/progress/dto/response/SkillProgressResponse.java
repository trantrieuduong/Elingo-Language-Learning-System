package com.elingo.progress.dto.response;

import lombok.Builder;

@Builder
public record SkillProgressResponse(
        FlashcardProgress flashcard,
        DictationProgress dictation,
        ShadowingProgress shadowing
) {

    @Builder
    public record FlashcardProgress(
            long masteredCards,
            long dueCards,
            long starredCards
    ) {
    }

    @Builder
    public record DictationProgress(
            long completedLessons,
            long inProgressLessons,
            long completedSegments,
            double averageScore,
            long totalAttempts,
            long totalStudyMinutes
    ) {
    }

    @Builder
    public record ShadowingProgress(
            long completedLessons,
            long inProgressLessons,
            long completedSegments,
            double averageScore,
            long totalAttempts,
            long totalStudyMinutes
    ) {
    }
}
