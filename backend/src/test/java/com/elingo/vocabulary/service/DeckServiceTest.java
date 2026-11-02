package com.elingo.vocabulary.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.progress.service.ProgressService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.config.FlashcardProperties;
import com.elingo.vocabulary.dto.request.DeckSearchRequest;
import com.elingo.vocabulary.dto.request.UserDeckRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.mapper.DeckMapper;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.impl.DeckServiceImpl;
import com.elingo.vocabulary.support.TestDeckAccessView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class DeckServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicMapper topicMapper;

    @Mock
    private DeckMapper deckMapper;

    @Mock
    private ProgressService progressService;

    @Mock
    private DeckAccessPolicy accessPolicy;

    @Mock
    private FlashcardProperties flashcardProperties;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeckServiceImpl deckService;

    private static final Long USER_ID = 1L;
    private static final Long DECK_ID = 10L;
    private static final int PAGE_SIZE = 20;
    private static final int MAX_DECKS_PER_USER = 20;

    @BeforeEach
    void setUp() {
        FlashcardProperties.UserDeckLimit limits = new FlashcardProperties.UserDeckLimit();
        limits.setMaxDecksPerUser(MAX_DECKS_PER_USER);
        lenient().when(flashcardProperties.getUserDeckLimit()).thenReturn(limits);
    }

    // ─────────────────────── helpers ───────────────────────

    private DeckResponse buildDeckResponse(String title) {
        return new DeckResponse(DECK_ID, title, "slug", "desc", false, null,
                DeckStatus.PUBLISHED, OwnerType.SYSTEM, null, 1, 10, 0, null,
                Set.of(), Set.of());
    }

    private DeckRepository.DeckAccessView accessView(Long deckId) {
        return TestDeckAccessView.published(deckId);
    }

    private Deck userDeck(String title) {
        return Deck.builder()
                .id(DECK_ID)
                .title(title)
                .slug("my-deck")
                .ownerType(OwnerType.USER)
                .status(DeckStatus.DRAFT)
                .build();
    }

    // ─────────────────────── getPublishedDecks ───────────────────────

    @Nested
    @DisplayName("getPublishedDecks")
    class GetPublishedDecksTests {

        @Test
        @DisplayName("Get published decks successfully: no filter returns first page")
        void getPublishedDecks_Success_NoFilter() {
            DeckResponse deck1 = buildDeckResponse("Animals");
            DeckResponse deck2 = buildDeckResponse("Colors");
            Page<Deck> entityPage = new PageImpl<>(
                    List.of(new Deck(), new Deck()), PageRequest.of(0, PAGE_SIZE), 2);

            when(deckRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);
            when(deckMapper.toDeckResponse(any(), eq(0))).thenReturn(deck1, deck2);

            DeckSearchRequest request = new DeckSearchRequest(null, null, null, null, null);
            PageResponse<DeckResponse> result =
                    deckService.getPublishedDecks(request, PageRequest.of(0, PAGE_SIZE), USER_ID);

            assertThat(result.content()).hasSize(2);
            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(PAGE_SIZE);
            assertThat(result.isLast()).isTrue();
            verify(deckRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Get published decks successfully: learned count is attached per deck")
        void getPublishedDecks_Success_LearnedCountAttached() {
            Deck animals = Deck.builder().id(1L).build();
            Deck colors = Deck.builder().id(2L).build();
            DeckResponse animalsResponse = buildDeckResponse("Animals");
            DeckResponse colorsResponse = buildDeckResponse("Colors");
            Page<Deck> entityPage = new PageImpl<>(List.of(animals, colors), PageRequest.of(0, PAGE_SIZE), 2);

            when(deckRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);
            when(progressService.getLearnedCardCountByDeck(USER_ID, List.of(1L, 2L)))
                    .thenReturn(Map.of(1L, 7));
            when(deckMapper.toDeckResponse(animals, 7)).thenReturn(animalsResponse);
            when(deckMapper.toDeckResponse(colors, 0)).thenReturn(colorsResponse);

            PageResponse<DeckResponse> result = deckService.getPublishedDecks(
                    new DeckSearchRequest(null, null, null, null, null),
                    PageRequest.of(0, PAGE_SIZE), USER_ID);

            assertThat(result.content()).containsExactly(animalsResponse, colorsResponse);
        }

        @Test
        @DisplayName("Get published decks successfully: anonymous user gets no learned count lookup")
        void getPublishedDecks_Success_Anonymous() {
            Page<Deck> entityPage = new PageImpl<>(List.of(new Deck()), PageRequest.of(0, PAGE_SIZE), 1);
            DeckResponse response = buildDeckResponse("Animals");

            when(deckRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);
            when(deckMapper.toDeckResponse(any(), eq(null))).thenReturn(response);

            PageResponse<DeckResponse> result = deckService.getPublishedDecks(
                    new DeckSearchRequest(null, null, null, null, null),
                    PageRequest.of(0, PAGE_SIZE), null);

            assertThat(result.content()).containsExactly(response);
            verifyNoInteractions(progressService);
        }

        @Test
        @DisplayName("Get published decks successfully: filter by CEFR level ID is passed to repository")
        void getPublishedDecks_Success_FilterByCefrCode() {
            DeckResponse b2Deck = buildDeckResponse("B2 Vocabulary");
            Page<Deck> entityPage = new PageImpl<>(List.of(new Deck()), PageRequest.of(0, PAGE_SIZE), 1);

            when(deckRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);
            when(deckMapper.toDeckResponse(any(), eq(0))).thenReturn(b2Deck);

            DeckSearchRequest request = new DeckSearchRequest(null, null, Set.of(1L), null, null);
            PageResponse<DeckResponse> result =
                    deckService.getPublishedDecks(request, PageRequest.of(0, PAGE_SIZE), USER_ID);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().title()).isEqualTo("B2 Vocabulary");
            verify(deckRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Get published decks successfully: filter by tag ID is passed to repository")
        void getPublishedDecks_Success_FilterByTagCode() {
            Page<Deck> entityPage = new PageImpl<>(List.of(), PageRequest.of(0, PAGE_SIZE), 0);

            when(deckRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);

            DeckSearchRequest request = new DeckSearchRequest(null, Set.of(1L), null, null, null);
            PageResponse<DeckResponse> result =
                    deckService.getPublishedDecks(request, PageRequest.of(0, PAGE_SIZE), USER_ID);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            verify(deckRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Get published decks successfully: filter by keyword is passed to repository")
        void getPublishedDecks_Success_FilterByKeyword() {
            DeckResponse deck = buildDeckResponse("English Animals");
            Page<Deck> entityPage = new PageImpl<>(List.of(new Deck()), PageRequest.of(0, PAGE_SIZE), 1);

            when(deckRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);
            when(deckMapper.toDeckResponse(any(), eq(0))).thenReturn(deck);

            DeckSearchRequest request = new DeckSearchRequest("animals", null, null, null, null);
            PageResponse<DeckResponse> result =
                    deckService.getPublishedDecks(request, PageRequest.of(0, PAGE_SIZE), USER_ID);

            assertThat(result.content()).hasSize(1);
            verify(deckRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Get published decks successfully: filter by premium is passed to repository")
        void getPublishedDecks_Success_FilterByPremium() {
            Page<Deck> entityPage = new PageImpl<>(List.of(), PageRequest.of(0, PAGE_SIZE), 0);

            when(deckRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);

            DeckSearchRequest request = new DeckSearchRequest(null, null, null, true, null);
            PageResponse<DeckResponse> result =
                    deckService.getPublishedDecks(request, PageRequest.of(0, PAGE_SIZE), USER_ID);

            assertThat(result.content()).isEmpty();
        }

        @Test
        @DisplayName("Get published decks successfully: returns empty page when no deck matches filter")
        void getPublishedDecks_Success_ReturnsEmptyPage() {
            Page<Deck> entityPage = new PageImpl<>(List.of(), PageRequest.of(0, PAGE_SIZE), 0);

            when(deckRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);

            DeckSearchRequest request = new DeckSearchRequest(null, null, Set.of(2L), null, null);
            PageResponse<DeckResponse> result =
                    deckService.getPublishedDecks(request, PageRequest.of(0, PAGE_SIZE), USER_ID);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            assertThat(result.totalPages()).isZero();
            assertThat(result.isLast()).isTrue();
        }
    }

    // ─────────────────────── getTopicsByDeckId ───────────────────────

    @Nested
    @DisplayName("getTopicsByDeckId")
    class GetTopicsByDeckIdTests {

        @Test
        @DisplayName("Get topics by deck id successfully: learned count is attached per topic")
        void getTopicsByDeckId_Success() {
            Topic animals = Topic.builder().id(1L).name("Animals").slug("animals").order(1).build();
            Topic colors = Topic.builder().id(2L).name("Colors").slug("colors").order(2).build();
            TopicResponse animalsResponse = new TopicResponse(1L, "Animals", "animals", 1, 5, 2);
            TopicResponse colorsResponse = new TopicResponse(2L, "Colors", "colors", 2, 5, 0);

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(topicRepository.findByDeckId(eq(DECK_ID), any(Sort.class))).thenReturn(List.of(animals, colors));
            when(progressService.getLearnedCardCountByTopic(USER_ID, List.of(1L, 2L)))
                    .thenReturn(Map.of(1L, 2));
            when(topicMapper.toTopicResponse(animals, 2)).thenReturn(animalsResponse);
            when(topicMapper.toTopicResponse(colors, 0)).thenReturn(colorsResponse);

            List<TopicResponse> result = deckService.getTopicsByDeckId(DECK_ID, USER_ID);

            assertThat(result).containsExactly(animalsResponse, colorsResponse);
            verify(accessPolicy).assertCanLearn(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Get topics by deck id successfully: empty deck returns empty list without progress lookup")
        void getTopicsByDeckId_Success_EmptyDeck() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(topicRepository.findByDeckId(eq(DECK_ID), any(Sort.class))).thenReturn(List.of());

            assertThat(deckService.getTopicsByDeckId(DECK_ID, USER_ID)).isEmpty();
            verifyNoInteractions(progressService);
        }

        @Test
        @DisplayName("Get topics by deck id successfully: anonymous user gets no learned count")
        void getTopicsByDeckId_Success_Anonymous() {
            Topic animals = Topic.builder().id(1L).name("Animals").slug("animals").order(1).build();
            TopicResponse response = new TopicResponse(1L, "Animals", "animals", 1, 5, null);

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(topicRepository.findByDeckId(eq(DECK_ID), any(Sort.class))).thenReturn(List.of(animals));
            when(topicMapper.toTopicResponse(animals, null)).thenReturn(response);

            assertThat(deckService.getTopicsByDeckId(DECK_ID, null)).containsExactly(response);
            verifyNoInteractions(progressService);
        }

        @Test
        @DisplayName("Get topics by deck id failed: deck not found")
        void getTopicsByDeckId_Fail_DeckNotFound() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.getTopicsByDeckId(DECK_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verify(topicRepository, never()).findByDeckId(any(), any());
        }

        @Test
        @DisplayName("Get topics by deck id failed: deck is not accessible")
        void getTopicsByDeckId_Fail_AccessDenied() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            doThrow(new AppException(AppError.DECK_NOT_FOUND))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> deckService.getTopicsByDeckId(DECK_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verify(topicRepository, never()).findByDeckId(any(), any());
        }

        @Test
        @DisplayName("Get topics by deck id failed: premium deck without subscription")
        void getTopicsByDeckId_Fail_PremiumRequired() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            doThrow(new AppException(AppError.PREMIUM_REQUIRED))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> deckService.getTopicsByDeckId(DECK_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PREMIUM_REQUIRED));
        }
    }

    // ─────────────────────── getTopicsByDeckSlug ───────────────────────

    @Nested
    @DisplayName("getTopicsByDeckSlug")
    class GetTopicsByDeckSlugTests {

        @Test
        @DisplayName("Get topics by deck slug successfully")
        void getTopicsByDeckSlug_Success() {
            Topic animals = Topic.builder().id(1L).name("Animals").slug("animals").order(1).build();
            TopicResponse response = new TopicResponse(1L, "Animals", "animals", 1, 5, 3);

            when(deckRepository.findAccessViewBySlug("oxford-3000"))
                    .thenReturn(Optional.of(accessView(DECK_ID)));
            when(topicRepository.findByDeckId(eq(DECK_ID), any(Sort.class))).thenReturn(List.of(animals));
            when(progressService.getLearnedCardCountByTopic(USER_ID, List.of(1L)))
                    .thenReturn(Map.of(1L, 3));
            when(topicMapper.toTopicResponse(animals, 3)).thenReturn(response);

            assertThat(deckService.getTopicsByDeckSlug("oxford-3000", USER_ID)).containsExactly(response);
            verify(accessPolicy).assertCanLearn(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Get topics by deck slug failed: deck not found")
        void getTopicsByDeckSlug_Fail_DeckNotFound() {
            when(deckRepository.findAccessViewBySlug("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.getTopicsByDeckSlug("missing", USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verify(topicRepository, never()).findByDeckId(any(), any());
        }

        @Test
        @DisplayName("Get topics by deck slug failed: premium deck without subscription")
        void getTopicsByDeckSlug_Fail_PremiumRequired() {
            when(deckRepository.findAccessViewBySlug("premium-deck"))
                    .thenReturn(Optional.of(accessView(DECK_ID)));
            doThrow(new AppException(AppError.PREMIUM_REQUIRED))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> deckService.getTopicsByDeckSlug("premium-deck", USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PREMIUM_REQUIRED));
        }
    }

    // ─────────────────────── getDeckById ───────────────────────

    @Nested
    @DisplayName("getDeckById")
    class GetDeckByIdTests {

        @Test
        @DisplayName("Get deck by id successfully: checks access and maps deck")
        void getDeckById_Success() {
            DeckRepository.DeckAccessView deckAccess = accessView(DECK_ID);
            Deck deck = userDeck("My deck");
            DeckResponse response = buildDeckResponse("My deck");

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(deckAccess));
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));
            when(deckMapper.toDeckResponse(deck)).thenReturn(response);

            assertThat(deckService.getDeckById(DECK_ID, USER_ID)).isSameAs(response);

            verify(accessPolicy).assertCanView(deckAccess, USER_ID);
            verify(deckRepository).findById(DECK_ID);
            verify(deckMapper).toDeckResponse(deck);
        }

        @Test
        @DisplayName("Get deck by id failed: caller cannot view deck")
        void getDeckById_Fail_AccessDenied() {
            DeckRepository.DeckAccessView deckAccess = accessView(DECK_ID);
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(deckAccess));
            doThrow(new AppException(AppError.DECK_NOT_FOUND))
                    .when(accessPolicy).assertCanView(deckAccess, USER_ID);

            assertThatThrownBy(() -> deckService.getDeckById(DECK_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verify(deckRepository, never()).findById(DECK_ID);
            verifyNoInteractions(deckMapper);
        }

        @Test
        @DisplayName("Get deck by id failed: deck does not exist")
        void getDeckById_Fail_DeckNotFound() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.getDeckById(DECK_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verifyNoInteractions(accessPolicy, deckMapper);
        }
    }

    // ─────────────────────── getUserDecks ───────────────────────

    @Nested
    @DisplayName("getUserDecks")
    class GetUserDecksTests {

        @Test
        @DisplayName("Get user decks successfully: maps every deck of the user")
        void getUserDecks_Success() {
            Deck first = userDeck("First");
            Deck second = userDeck("Second");
            DeckResponse firstResponse = buildDeckResponse("First");
            DeckResponse secondResponse = buildDeckResponse("Second");

            when(deckRepository.findAllByOwnerTypeAndOwnerId(OwnerType.USER, USER_ID))
                    .thenReturn(List.of(first, second));
            when(deckMapper.toDeckResponse(first)).thenReturn(firstResponse);
            when(deckMapper.toDeckResponse(second)).thenReturn(secondResponse);

            assertThat(deckService.getUserDecks(USER_ID)).containsExactly(firstResponse, secondResponse);
        }

        @Test
        @DisplayName("Get user decks successfully: returns empty list when the user owns no deck")
        void getUserDecks_Success_Empty() {
            when(deckRepository.findAllByOwnerTypeAndOwnerId(OwnerType.USER, USER_ID)).thenReturn(List.of());

            assertThat(deckService.getUserDecks(USER_ID)).isEmpty();
        }
    }

    // ─────────────────────── createUserDeck ───────────────────────

    @Nested
    @DisplayName("createUserDeck")
    class CreateUserDeckTests {

        @Test
        @DisplayName("Create user deck successfully: stores owner, slug and draft status")
        void createUserDeck_Success() {
            User owner = User.builder().id(USER_ID).username("tester").build();
            UserDeckRequest request = new UserDeckRequest("My Deck", "my description");
            DeckResponse response = buildDeckResponse("My Deck");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
            when(deckRepository.countByOwnerTypeAndOwnerId(OwnerType.USER, USER_ID)).thenReturn(0L);
            when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deckMapper.toDeckResponse(any(Deck.class))).thenReturn(response);

            assertThat(deckService.createUserDeck(USER_ID, request)).isSameAs(response);

            ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
            verify(deckRepository).save(captor.capture());
            Deck persisted = captor.getValue();
            assertThat(persisted.getTitle()).isEqualTo("My Deck");
            assertThat(persisted.getDescription()).isEqualTo("my description");
            assertThat(persisted.getSlug()).isNotBlank();
            assertThat(persisted.getIsPremium()).isFalse();
            assertThat(persisted.getOwnerType()).isEqualTo(OwnerType.USER);
            assertThat(persisted.getOwner()).isSameAs(owner);
        }

        @Test
        @DisplayName("Create user deck failed: user not found")
        void createUserDeck_Fail_UserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.createUserDeck(USER_ID, new UserDeckRequest("My Deck", null)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));

            verify(deckRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create user deck failed: deck limit reached")
        void createUserDeck_Fail_DeckLimitExceeded() {
            User owner = User.builder().id(USER_ID).username("tester").build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
            when(deckRepository.countByOwnerTypeAndOwnerId(OwnerType.USER, USER_ID))
                    .thenReturn((long) MAX_DECKS_PER_USER);

            assertThatThrownBy(() -> deckService.createUserDeck(USER_ID, new UserDeckRequest("My Deck", null)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_LIMIT_EXCEEDED));

            verify(deckRepository, never()).save(any());
        }
    }

    // ─────────────────────── updateUserDeck ───────────────────────

    @Nested
    @DisplayName("updateUserDeck")
    class UpdateUserDeckTests {

        @Test
        @DisplayName("Update user deck successfully")
        void updateUserDeck_Success() {
            Deck deck = userDeck("Old title");
            UserDeckRequest request = new UserDeckRequest("New title", "new desc");
            DeckResponse response = buildDeckResponse("New title");

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));
            when(deckMapper.toDeckResponse(deck)).thenReturn(response);

            assertThat(deckService.updateUserDeck(USER_ID, DECK_ID, request)).isSameAs(response);

            verify(deckMapper).updateDeck(deck, request);
            verify(accessPolicy).assertCanEdit(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Update user deck failed: deck not found")
        void updateUserDeck_Fail_DeckNotFound() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.updateUserDeck(USER_ID, DECK_ID,
                    new UserDeckRequest("New title", null)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verify(deckMapper, never()).updateDeck(any(), any());
        }

        @Test
        @DisplayName("Update user deck failed: user does not own the deck")
        void updateUserDeck_Fail_AccessDenied() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(accessPolicy).assertCanEdit(any(), eq(USER_ID));

            assertThatThrownBy(() -> deckService.updateUserDeck(USER_ID, DECK_ID,
                    new UserDeckRequest("New title", null)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));

            verify(deckRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Update user deck failed: deck entity disappeared")
        void updateUserDeck_Fail_EntityNotFound() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.updateUserDeck(USER_ID, DECK_ID,
                    new UserDeckRequest("New title", null)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verify(deckMapper, never()).updateDeck(any(), any());
        }
    }

    // ─────────────────────── setUserDeckPublished ───────────────────────

    @Nested
    @DisplayName("setUserDeckPublished")
    class SetUserDeckPublishedTests {

        @Test
        @DisplayName("Publish deck successfully: status and publishedAt are set")
        void setUserDeckPublished_Publish_Success() {
            Deck deck = Deck.builder()
                    .id(DECK_ID)
                    .status(DeckStatus.DRAFT)
                    .cardCount(5)
                    .build();
            DeckResponse response = buildDeckResponse("My Deck");

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));
            when(deckMapper.toDeckResponse(deck)).thenReturn(response);

            assertThat(deckService.setUserDeckPublished(USER_ID, DECK_ID, true)).isSameAs(response);
            assertThat(deck.getStatus()).isEqualTo(DeckStatus.PUBLISHED);
            assertThat(deck.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Publish deck successfully: an already published deck keeps its publishedAt")
        void setUserDeckPublished_Publish_AlreadyPublished() {
            LocalDateTime publishedAt = LocalDateTime.now().minusDays(3);
            Deck deck = Deck.builder()
                    .id(DECK_ID)
                    .status(DeckStatus.PUBLISHED)
                    .cardCount(5)
                    .publishedAt(publishedAt)
                    .build();

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));

            deckService.setUserDeckPublished(USER_ID, DECK_ID, true);

            assertThat(deck.getPublishedAt()).isEqualTo(publishedAt);
        }

        @Test
        @DisplayName("Publish deck failed: deck has no card")
        void setUserDeckPublished_Publish_Fail_DeckEmpty() {
            Deck deck = Deck.builder()
                    .id(DECK_ID)
                    .status(DeckStatus.DRAFT)
                    .cardCount(0)
                    .build();

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));

            assertThatThrownBy(() -> deckService.setUserDeckPublished(USER_ID, DECK_ID, true))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_EMPTY));

            assertThat(deck.getStatus()).isEqualTo(DeckStatus.DRAFT);
            verify(deckMapper, never()).toDeckResponse(any(Deck.class));
        }

        @Test
        @DisplayName("Unpublish deck successfully: status back to draft and publishedAt cleared")
        void setUserDeckPublished_Unpublish_Success() {
            Deck deck = Deck.builder()
                    .id(DECK_ID)
                    .status(DeckStatus.PUBLISHED)
                    .cardCount(5)
                    .publishedAt(LocalDateTime.now())
                    .build();
            DeckResponse response = buildDeckResponse("My Deck");

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));
            when(deckMapper.toDeckResponse(deck)).thenReturn(response);

            assertThat(deckService.setUserDeckPublished(USER_ID, DECK_ID, false)).isSameAs(response);
            assertThat(deck.getStatus()).isEqualTo(DeckStatus.DRAFT);
            assertThat(deck.getPublishedAt()).isNull();
        }

        @Test
        @DisplayName("Set publication failed: deck not found")
        void setUserDeckPublished_Fail_DeckNotFound() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.setUserDeckPublished(USER_ID, DECK_ID, true))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));
        }

        @Test
        @DisplayName("Set publication failed: user does not own the deck")
        void setUserDeckPublished_Fail_AccessDenied() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(accessPolicy).assertCanEdit(any(), eq(USER_ID));

            assertThatThrownBy(() -> deckService.setUserDeckPublished(USER_ID, DECK_ID, true))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));
        }
    }

    // ─────────────────────── deleteUserDeck ───────────────────────

    @Nested
    @DisplayName("deleteUserDeck")
    class DeleteUserDeckTests {

        @Test
        @DisplayName("Delete user deck successfully")
        void deleteUserDeck_Success() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));

            deckService.deleteUserDeck(USER_ID, DECK_ID);

            verify(deckRepository).deleteById(DECK_ID);
            verify(accessPolicy).assertCanEdit(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Delete user deck failed: deck not found")
        void deleteUserDeck_Fail_DeckNotFound() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.deleteUserDeck(USER_ID, DECK_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verify(deckRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Delete user deck failed: user does not own the deck")
        void deleteUserDeck_Fail_AccessDenied() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(accessPolicy).assertCanEdit(any(), eq(USER_ID));

            assertThatThrownBy(() -> deckService.deleteUserDeck(USER_ID, DECK_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));

            verify(deckRepository, never()).deleteById(any());
        }
    }
}
