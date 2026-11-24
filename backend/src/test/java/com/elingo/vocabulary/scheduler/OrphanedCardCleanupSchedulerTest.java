package com.elingo.vocabulary.scheduler;

import com.elingo.vocabulary.dto.response.OrphanedCardCleanupResponse;
import com.elingo.vocabulary.service.CardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class OrphanedCardCleanupSchedulerTest {

    @Mock
    private CardService cardService;

    @InjectMocks
    private OrphanedCardCleanupScheduler scheduler;

    @Nested
    @DisplayName("cleanupOrphanedCards")
    class CleanupOrphanedCardsTests {

        @Test
        @DisplayName("Should log cleanup details when safe orphaned cards are deleted")
        void cleanupOrphanedCards_Success_WhenCardsDeleted() {
            OrphanedCardCleanupResponse response = new OrphanedCardCleanupResponse(10, 5, 3);
            when(cardService.cleanupOrphanedCards()).thenReturn(response);

            scheduler.cleanupOrphanedCards();

            verify(cardService).cleanupOrphanedCards();
        }

        @Test
        @DisplayName("Should execute normally when no orphaned cards need deletion")
        void cleanupOrphanedCards_Success_WhenNoCardsDeleted() {
            OrphanedCardCleanupResponse response = new OrphanedCardCleanupResponse(0, 0, 0);
            when(cardService.cleanupOrphanedCards()).thenReturn(response);

            scheduler.cleanupOrphanedCards();

            verify(cardService).cleanupOrphanedCards();
        }

        @Test
        @DisplayName("Should catch and handle exception gracefully when service fails")
        void cleanupOrphanedCards_Exception_ShouldHandleGracefully() {
            when(cardService.cleanupOrphanedCards()).thenThrow(new RuntimeException("Database connection failure"));

            scheduler.cleanupOrphanedCards();

            verify(cardService).cleanupOrphanedCards();
        }
    }
}
