package com.elingo.vocabulary.scheduler;

import com.elingo.vocabulary.dto.response.OrphanedCardCleanupResponse;
import com.elingo.vocabulary.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "ORPHANED-CARD-SCHEDULER")
public class OrphanedCardCleanupScheduler {

    private final CardService cardService;

    /**
     * Tác vụ định kỳ (mặc định 02:00 sáng mỗi Chủ nhật) quét và dọn dẹp các thẻ mồ côi
     * không còn lưu vết học tập trong user_card_states.
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void cleanupOrphanedCards() {
        log.info("Scheduled orphaned cards cleanup job started");
        try {
            OrphanedCardCleanupResponse response = cardService.cleanupOrphanedCards();
            if (response.deletedCardCount() > 0) {
                log.info("Scheduled orphaned cards cleanup completed scanned={} deletedCards={} deletedFiles={}",
                        response.scannedCount(), response.deletedCardCount(), response.deletedFileCount());
            } else {
                log.info("Scheduled orphaned cards cleanup completed no unused orphaned cards found");
            }
        } catch (Exception e) {
            log.error("Scheduled orphaned cards cleanup job failed", e);
        }
    }
}
