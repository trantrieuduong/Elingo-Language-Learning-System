package com.elingo.admin.content.service;

import com.elingo.vocabulary.dto.response.OrphanedCardCleanupResponse;
import com.elingo.vocabulary.dto.response.OrphanedCardStatsResponse;
import com.elingo.admin.content.service.impl.AdminOrphanedCardServiceImpl;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.service.CardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminOrphanedCardServiceTest {

    @Mock
    private CardService cardService;

    @InjectMocks
    private AdminOrphanedCardServiceImpl adminOrphanedCardService;

    @Nested
    @DisplayName("getStats")
    class GetStatsTests {

        @Test
        @DisplayName("Get orphaned card stats successfully delegating to cardService")
        void getStats_Success() {
            OrphanedCardStatsResponse expected = new OrphanedCardStatsResponse(12L, 10L, 2L);
            when(cardService.getOrphanedCardStats()).thenReturn(expected);

            OrphanedCardStatsResponse actual = adminOrphanedCardService.getStats();

            assertThat(actual).isEqualTo(expected);
            verify(cardService).getOrphanedCardStats();
        }
    }

    @Nested
    @DisplayName("getOrphanedCards")
    class GetOrphanedCardsTests {

        @Test
        @DisplayName("Get orphaned cards page successfully delegating to cardService")
        void getOrphanedCards_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<CardResponse> expected = new PageResponse<>(
                    List.of(), 1, 10, 0, 0, true
            );
            when(cardService.getOrphanedCards(pageable)).thenReturn(expected);

            PageResponse<CardResponse> actual = adminOrphanedCardService.getOrphanedCards(pageable);

            assertThat(actual).isEqualTo(expected);
            verify(cardService).getOrphanedCards(pageable);
        }

        @Test
        @DisplayName("Should pass sanitized pageable when sort is allowed")
        void getOrphanedCards_WhenSortAllowed_Success() {
            Pageable sortedPageable = PageRequest.of(0, 10, Sort.by("createdAt"));
            PageResponse<CardResponse> expected = new PageResponse<>(List.of(), 1, 10, 0, 0, true);
            when(cardService.getOrphanedCards(sortedPageable)).thenReturn(expected);

            PageResponse<CardResponse> actual = adminOrphanedCardService.getOrphanedCards(sortedPageable);

            assertThat(actual).isEqualTo(expected);
            verify(cardService).getOrphanedCards(sortedPageable);
        }

        @Test
        @DisplayName("Should throw INVALID_SORT when sort field is not allowed")
        void getOrphanedCards_WhenSortNotAllowed_ShouldThrowInvalidSort() {
            Pageable invalidSortPageable = PageRequest.of(0, 10, Sort.by("invalidColumn"));

            assertThatThrownBy(() -> adminOrphanedCardService.getOrphanedCards(invalidSortPageable))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_SORT));
        }
    }

    @Nested
    @DisplayName("cleanupOrphanedCards")
    class CleanupOrphanedCardsTests {

        @Test
        @DisplayName("Cleanup orphaned cards successfully delegating to cardService")
        void cleanupOrphanedCards_Success() {
            OrphanedCardCleanupResponse expected = new OrphanedCardCleanupResponse(5, 5, 2);
            when(cardService.cleanupOrphanedCards()).thenReturn(expected);

            OrphanedCardCleanupResponse actual = adminOrphanedCardService.cleanupOrphanedCards();

            assertThat(actual).isEqualTo(expected);
            verify(cardService).cleanupOrphanedCards();
        }
    }
}
