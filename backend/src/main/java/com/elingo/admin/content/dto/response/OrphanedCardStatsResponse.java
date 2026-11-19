package com.elingo.admin.content.dto.response;

public record OrphanedCardStatsResponse(
        long totalOrphanedCards,
        long safeToDeleteCards,
        long retainedInStudyCards
) {
}
