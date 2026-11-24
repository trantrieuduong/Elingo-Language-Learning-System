package com.elingo.vocabulary.dto.response;

public record OrphanedCardStatsResponse(
        long totalOrphanedCards,
        long safeToDeleteCards,
        long retainedInStudyCards
) {
}
