package com.elingo.progress.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record DailyStudyProgressResponse(
        LocalDate date,
        long flashcardCount,
        long dictationCount,
        long shadowingCount,
        long battleCount,
        long totalActivities,
        long xpEarned
) {
}
