package com.elingo.vocabulary.dto.response;

public record OrphanedCardCleanupResponse(
        int scannedCount,
        int deletedCardCount,
        int deletedFileCount
) {
}
