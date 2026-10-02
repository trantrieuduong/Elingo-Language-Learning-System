package com.elingo.vocabulary.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.request.GetPublishedDecksRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.mapper.DeckMapper;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.service.impl.DeckServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class DeckServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private DeckMapper deckMapper;

    @InjectMocks
    private DeckServiceImpl deckService;

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 9;

    private DeckResponse buildDeckResponse(String title) {
        return new DeckResponse(1L, title, "slug", "desc", false, null,
                DeckStatus.PUBLISHED, OwnerType.SYSTEM, 0, 0, Set.of(), Set.of());
    }

    @Nested
    @DisplayName("getPublishedDecks")
    class GetPublishedDecksTests {

        @Test
        @DisplayName("Get published decks successfully: no filter returns first page")
        void getPublishedDecks_Success_NoFilter() {
            DeckResponse deck1 = buildDeckResponse("Animals");
            DeckResponse deck2 = buildDeckResponse("Colors");
            Page<com.elingo.vocabulary.entity.Deck> entityPage =
                    new PageImpl<>(
                            List.of(
                                    new com.elingo.vocabulary.entity.Deck(),
                                    new com.elingo.vocabulary.entity.Deck()
                            ),
                            PageRequest.of(0, PAGE_SIZE),
                            2);

            when(deckRepository.findPublishedWithFilters(isNull(), isNull(), isNull(),
                    any(Pageable.class))).thenReturn(entityPage);
            when(deckMapper.toDeckResponse(any())).thenReturn(deck1, deck2);

            GetPublishedDecksRequest request = new GetPublishedDecksRequest(null, null, null, 0);
            PageResponse<DeckResponse> result = deckService.getAllPublishedDecks(USER_ID, request);

            assertThat(result.content()).hasSize(2);
            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.page()).isEqualTo(0);
            assertThat(result.size()).isEqualTo(PAGE_SIZE);
            assertThat(result.isLast()).isTrue();
            verify(deckRepository).findPublishedWithFilters(isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Get published decks successfully: filter by CEFR code is passed to repository")
        void getPublishedDecks_Success_FilterByCefrCode() {
            DeckResponse b2Deck = buildDeckResponse("B2 Vocabulary");
            Page<com.elingo.vocabulary.entity.Deck> entityPage =
                    new PageImpl<>(
                        List.of(
                                new com.elingo.vocabulary.entity.Deck()
                        ),
                        PageRequest.of(0, PAGE_SIZE),
                    1);

            when(deckRepository.findPublishedWithFilters(eq("B2"), isNull(), isNull(),
                    any(Pageable.class))).thenReturn(entityPage);
            when(deckMapper.toDeckResponse(any())).thenReturn(b2Deck);

            GetPublishedDecksRequest request = new GetPublishedDecksRequest("B2", null, null, 0);
            PageResponse<DeckResponse> result = deckService.getAllPublishedDecks(USER_ID, request);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().title()).isEqualTo("B2 Vocabulary");
            verify(deckRepository).findPublishedWithFilters(eq("B2"), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Get published decks successfully: filter by tag code is passed to repository")
        void getPublishedDecks_Success_FilterByTagCode() {
            Page<com.elingo.vocabulary.entity.Deck> entityPage =
                    new PageImpl<>(List.of(), PageRequest.of(0, PAGE_SIZE), 0);

            when(deckRepository.findPublishedWithFilters(isNull(), eq("TRAVEL"), isNull(),
                    any(Pageable.class))).thenReturn(entityPage);

            GetPublishedDecksRequest request = new GetPublishedDecksRequest(null, "TRAVEL", null, 0);
            PageResponse<DeckResponse> result = deckService.getAllPublishedDecks(USER_ID, request);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isEqualTo(0);
            verify(deckRepository).findPublishedWithFilters(isNull(), eq("TRAVEL"), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Get published decks successfully: filter by keyword is passed to repository")
        void getPublishedDecks_Success_FilterByKeyword() {
            DeckResponse deck = buildDeckResponse("English Animals");
            Page<com.elingo.vocabulary.entity.Deck> entityPage =
                    new PageImpl<>(List.of(new com.elingo.vocabulary.entity.Deck()),
                            PageRequest.of(0, PAGE_SIZE), 1);

            when(deckRepository.findPublishedWithFilters(isNull(), isNull(), eq("animals"),
                    any(Pageable.class))).thenReturn(entityPage);
            when(deckMapper.toDeckResponse(any())).thenReturn(deck);

            GetPublishedDecksRequest request = new GetPublishedDecksRequest(null, null, "animals", 0);
            PageResponse<DeckResponse> result = deckService.getAllPublishedDecks(USER_ID, request);

            assertThat(result.content()).hasSize(1);
            verify(deckRepository).findPublishedWithFilters(isNull(), isNull(), eq("animals"), any(Pageable.class));
        }

        @Test
        @DisplayName("Get published decks successfully: blank keyword is normalized to null")
        void getPublishedDecks_Success_BlankKeywordNormalizedToNull() {
            Page<com.elingo.vocabulary.entity.Deck> entityPage =
                    new PageImpl<>(List.of(), PageRequest.of(0, PAGE_SIZE), 0);

            when(deckRepository.findPublishedWithFilters(isNull(), isNull(), isNull(),
                    any(Pageable.class))).thenReturn(entityPage);

            GetPublishedDecksRequest request = new GetPublishedDecksRequest(null, null, "   ", 0);
            deckService.getAllPublishedDecks(USER_ID, request);

            // Blank keyword must be converted to null before hitting the repository
            verify(deckRepository).findPublishedWithFilters(isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Get published decks successfully: returns empty page when no deck matches filter")
        void getPublishedDecks_Success_ReturnsEmptyPage() {
            Page<com.elingo.vocabulary.entity.Deck> entityPage =
                    new PageImpl<>(List.of(), PageRequest.of(0, PAGE_SIZE), 0);

            when(deckRepository.findPublishedWithFilters(eq("C2"), isNull(), isNull(),
                    any(Pageable.class))).thenReturn(entityPage);

            GetPublishedDecksRequest request = new GetPublishedDecksRequest("C2", null, null, 0);
            PageResponse<DeckResponse> result = deckService.getAllPublishedDecks(USER_ID, request);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isEqualTo(0);
            assertThat(result.totalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("Get published decks successfully: page size is always fixed at 9")
        void getPublishedDecks_Success_PageSizeAlwaysNine() {
            Page<com.elingo.vocabulary.entity.Deck> entityPage =
                    new PageImpl<>(List.of(), PageRequest.of(0, PAGE_SIZE), 0);

            when(deckRepository.findPublishedWithFilters(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(entityPage);

            GetPublishedDecksRequest request = new GetPublishedDecksRequest(null, null, null, 0);
            PageResponse<DeckResponse> result = deckService.getAllPublishedDecks(USER_ID, request);

            assertThat(result.size()).isEqualTo(PAGE_SIZE);
        }// test thừa: chỉ kiểm tra xem Mockito có hoạt động đúng như đã cấu hình hay không, không hề kiểm tra logic của deckService
    }
}
