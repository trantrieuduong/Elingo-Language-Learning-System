package com.elingo.admin.content.dto.response;

public record OrphanedCardCleanupResponse(
        int scannedCount,
        int deletedCardCount,
        int deletedFileCount
) {
}
