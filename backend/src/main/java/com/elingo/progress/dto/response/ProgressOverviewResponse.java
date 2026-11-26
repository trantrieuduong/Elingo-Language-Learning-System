package com.elingo.progress.dto.response;

import lombok.Builder;

@Builder
public record ProgressOverviewResponse(
        long totalLearnedCards,
        long totalCompletedSegments,
        long totalStudyMinutes
) {
}
