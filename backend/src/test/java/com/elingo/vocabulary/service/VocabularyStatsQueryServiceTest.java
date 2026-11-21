package com.elingo.vocabulary.service;

import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.service.impl.VocabularyStatsQueryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class VocabularyStatsQueryServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @InjectMocks
    private VocabularyStatsQueryServiceImpl vocabularyStatsQueryService;

    @Nested
    @DisplayName("countDecksByStatus")
    class CountDecksByStatusTests {

        @Test
        @DisplayName("Should delegate to deckRepository.countDecksByStatus")
        void countDecksByStatus_Success() {
            DeckRepository.StatusCountProjection proj = mock(DeckRepository.StatusCountProjection.class);
            when(proj.getStatus()).thenReturn(DeckStatus.PUBLISHED);
            when(proj.getCountVal()).thenReturn(10L);
            when(deckRepository.countDecksByStatus()).thenReturn(List.of(proj));

            List<DeckRepository.StatusCountProjection> result = vocabularyStatsQueryService.countDecksByStatus();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(DeckStatus.PUBLISHED);
            assertThat(result.get(0).getCountVal()).isEqualTo(10L);
            verify(deckRepository).countDecksByStatus();
        }
    }

    @Nested
    @DisplayName("countDecksByOwnerType")
    class CountDecksByOwnerTypeTests {

        @Test
        @DisplayName("Should delegate to deckRepository.countDecksByOwnerType")
        void countDecksByOwnerType_Success() {
            DeckRepository.OwnerTypeCountProjection proj = mock(DeckRepository.OwnerTypeCountProjection.class);
            when(proj.getOwnerType()).thenReturn(OwnerType.SYSTEM);
            when(proj.getCountVal()).thenReturn(5L);
            when(deckRepository.countDecksByOwnerType()).thenReturn(List.of(proj));

            List<DeckRepository.OwnerTypeCountProjection> result = vocabularyStatsQueryService.countDecksByOwnerType();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOwnerType()).isEqualTo(OwnerType.SYSTEM);
            assertThat(result.get(0).getCountVal()).isEqualTo(5L);
            verify(deckRepository).countDecksByOwnerType();
        }
    }

    @Nested
    @DisplayName("countDecksByPremium")
    class CountDecksByPremiumTests {

        @Test
        @DisplayName("Should delegate to deckRepository.countDecksByPremium")
        void countDecksByPremium_Success() {
            DeckRepository.PremiumCountProjection proj = mock(DeckRepository.PremiumCountProjection.class);
            when(proj.getIsPremium()).thenReturn(true);
            when(proj.getCountVal()).thenReturn(8L);
            when(deckRepository.countDecksByPremium()).thenReturn(List.of(proj));

            List<DeckRepository.PremiumCountProjection> result = vocabularyStatsQueryService.countDecksByPremium();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsPremium()).isTrue();
            assertThat(result.get(0).getCountVal()).isEqualTo(8L);
            verify(deckRepository).countDecksByPremium();
        }
    }

    @Nested
    @DisplayName("getTopDecksByOwnerType")
    class GetTopDecksByOwnerTypeTests {

        @Test
        @DisplayName("Should call repository with owner type name and enforced minimum limit of 1")
        void getTopDecksByOwnerType_Success() {
            DeckRepository.TopDeckProjection proj = mock(DeckRepository.TopDeckProjection.class);
            when(proj.getDeckId()).thenReturn(1L);
            when(deckRepository.findTopDecksByOwnerType("SYSTEM", 10)).thenReturn(List.of(proj));

            List<DeckRepository.TopDeckProjection> result =
                    vocabularyStatsQueryService.getTopDecksByOwnerType(OwnerType.SYSTEM, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDeckId()).isEqualTo(1L);
            verify(deckRepository).findTopDecksByOwnerType("SYSTEM", 10);
        }

        @Test
        @DisplayName("Should enforce minimum limit of 1 when limit <= 0")
        void getTopDecksByOwnerType_EnforcesMinimumLimit() {
            when(deckRepository.findTopDecksByOwnerType("USER", 1)).thenReturn(List.of());

            List<DeckRepository.TopDeckProjection> result =
                    vocabularyStatsQueryService.getTopDecksByOwnerType(OwnerType.USER, 0);

            assertThat(result).isEmpty();
            verify(deckRepository).findTopDecksByOwnerType("USER", 1);
        }
    }
}
