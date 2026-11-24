package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.request.AdminDeckSearchRequest;
import com.elingo.vocabulary.dto.request.SystemDeckRequest;
import com.elingo.vocabulary.dto.request.UpdateDeckStatusRequest;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.enums.SystemFileModule;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.file.util.MediaKeyResolver;
import com.elingo.progress.service.ProgressService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.config.FlashcardProperties;
import com.elingo.vocabulary.dto.request.DeckSearchRequest;
import com.elingo.vocabulary.dto.request.UserDeckRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.CefrLevel;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.entity.Tag;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.mapper.DeckMapper;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.repository.CefrLevelRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TagRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.impl.DeckServiceImpl;
import com.elingo.vocabulary.support.TestDeckAccessView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@org.junit.jupiter.api.Tag("unit")
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

    @Mock
    private TagRepository tagRepository;

    @Mock
    private CefrLevelRepository cefrLevelRepository;

    @Mock
    private MediaKeyResolver mediaKeyResolver;

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

    private Deck systemDeck(String title) {
        return Deck.builder()
                .id(DECK_ID)
                .title(title)
                .slug("system-deck")
                .ownerType(OwnerType.SYSTEM)
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
            DeckSearchRequest request = new DeckSearchRequest(null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("createdAt").descending());
            Deck deck = userDeck("A");
            Page<Deck> page = new PageImpl<>(List.of(deck), pageable, 1);
            DeckResponse response = buildDeckResponse("A");

            when(deckRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(deckMapper.toDeckResponse(deck, null)).thenReturn(response);

            PageResponse<DeckResponse> result = deckService.getPublishedDecks(request, pageable, null);

            assertThat(result.content()).containsExactly(response);
            assertThat(result.totalElements()).isEqualTo(1L);
            verifyNoInteractions(progressService);
        }

        @Test
        @DisplayName("Get published decks successfully: attaches user learning progress when logged in")
        void getPublishedDecks_Success_WithUserProgress() {
            DeckSearchRequest request = new DeckSearchRequest(null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, PAGE_SIZE);
            Deck deck = userDeck("A");
            Page<Deck> page = new PageImpl<>(List.of(deck), pageable, 1);
            DeckResponse response = buildDeckResponse("A");

            when(deckRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(progressService.getLearnedCardCountByDeck(USER_ID, List.of(DECK_ID)))
                    .thenReturn(Map.of(DECK_ID, 7));
            when(deckMapper.toDeckResponse(deck, 7)).thenReturn(response);

            PageResponse<DeckResponse> result = deckService.getPublishedDecks(request, pageable, USER_ID);

            assertThat(result.content()).containsExactly(response);
            verify(progressService).getLearnedCardCountByDeck(USER_ID, List.of(DECK_ID));
        }

        @Test
        @DisplayName("Get published decks successfully: empty page skips progress lookup")
        void getPublishedDecks_Success_EmptyPage() {
            DeckSearchRequest request = new DeckSearchRequest(null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, PAGE_SIZE);
            Page<Deck> empty = new PageImpl<>(List.of(), pageable, 0);

            when(deckRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(empty);

            PageResponse<DeckResponse> result = deckService.getPublishedDecks(request, pageable, null);

            assertThat(result.content()).isEmpty();
            verifyNoInteractions(progressService);
        }
    }

    // ─────────────────────── getDeckById ───────────────────────

    @Nested
    @DisplayName("getDeckById")
    class GetDeckByIdTests {

        @Test
        @DisplayName("Get deck by ID successfully: anonymous user")
        void getDeckById_Success_Anonymous() {
            Deck deck = userDeck("A");
            DeckResponse response = buildDeckResponse("A");

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));
            when(deckMapper.toDeckResponse(deck)).thenReturn(response);

            DeckResponse result = deckService.getDeckById(DECK_ID, null);

            assertThat(result).isSameAs(response);
            verify(accessPolicy).assertCanView(any(), eq(null));
        }

        @Test
        @DisplayName("Get deck by ID successfully: logged-in user")
        void getDeckById_Success_LoggedIn() {
            Deck deck = userDeck("A");
            DeckResponse response = buildDeckResponse("A");

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));
            when(deckMapper.toDeckResponse(deck)).thenReturn(response);

            DeckResponse result = deckService.getDeckById(DECK_ID, USER_ID);

            assertThat(result).isSameAs(response);
            verify(accessPolicy).assertCanView(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Get deck by ID failed: access view not found")
        void getDeckById_Fail_AccessViewNotFound() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.getDeckById(DECK_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_FOUND));

            verifyNoInteractions(accessPolicy);
        }

        @Test
        @DisplayName("Get deck by ID failed: access denied by policy")
        void getDeckById_Fail_AccessDenied() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(accessPolicy).assertCanView(any(), eq(USER_ID));

            assertThatThrownBy(() -> deckService.getDeckById(DECK_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_ACCESS_DENIED));

            verify(deckRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Get deck by ID failed: deck entity disappeared")
        void getDeckById_Fail_EntityNotFound() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.getDeckById(DECK_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_FOUND));
        }
    }

    // ─────────────────────── getTopicsByDeckId ───────────────────────

    @Nested
    @DisplayName("getTopicsByDeckId")
    class GetTopicsByDeckIdTests {

        @Test
        @DisplayName("Get topics by deck ID successfully: maps topics with user learning progress")
        void getTopicsByDeckId_Success_WithUserProgress() {
            Topic topic = Topic.builder().id(20L).name("Topic 1").build();
            TopicResponse topicResponse = new TopicResponse(20L, "Topic 1", "topic-1", 1, 10, 3);

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(topicRepository.findByDeckId(eq(DECK_ID), any(Sort.class))).thenReturn(List.of(topic));
            when(progressService.getLearnedCardCountByTopic(USER_ID, List.of(20L)))
                    .thenReturn(Map.of(20L, 3));
            when(topicMapper.toTopicResponse(topic, 3)).thenReturn(topicResponse);

            List<TopicResponse> result = deckService.getTopicsByDeckId(DECK_ID, USER_ID);

            assertThat(result).containsExactly(topicResponse);
            verify(accessPolicy).assertCanLearn(any(), eq(USER_ID));
            verify(progressService).getLearnedCardCountByTopic(USER_ID, List.of(20L));
        }

        @Test
        @DisplayName("Get topics by deck ID successfully: anonymous user skips progress lookup")
        void getTopicsByDeckId_Success_Anonymous() {
            Topic topic = Topic.builder().id(20L).name("Topic 1").build();
            TopicResponse topicResponse = new TopicResponse(20L, "Topic 1", "topic-1", 1, 10, null);

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(topicRepository.findByDeckId(eq(DECK_ID), any(Sort.class))).thenReturn(List.of(topic));
            when(topicMapper.toTopicResponse(topic, null)).thenReturn(topicResponse);

            List<TopicResponse> result = deckService.getTopicsByDeckId(DECK_ID, null);

            assertThat(result).containsExactly(topicResponse);
            verify(accessPolicy).assertCanLearn(any(), eq(null));
            verifyNoInteractions(progressService);
        }

        @Test
        @DisplayName("Get topics by deck ID successfully: returns empty list when deck has no topics")
        void getTopicsByDeckId_Success_Empty() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(topicRepository.findByDeckId(eq(DECK_ID), any(Sort.class))).thenReturn(List.of());

            List<TopicResponse> result = deckService.getTopicsByDeckId(DECK_ID, USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(progressService);
        }

        @Test
        @DisplayName("Get topics by deck ID failed: deck not found")
        void getTopicsByDeckId_Fail_DeckNotFound() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.getTopicsByDeckId(DECK_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_FOUND));

            verifyNoInteractions(accessPolicy, topicRepository);
        }

        @Test
        @DisplayName("Get topics by deck ID failed: access denied")
        void getTopicsByDeckId_Fail_AccessDenied() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> deckService.getTopicsByDeckId(DECK_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_ACCESS_DENIED));

            verifyNoInteractions(topicRepository);
        }
    }

    // ─────────────────────── getTopicsByDeckSlug ───────────────────────

    @Nested
    @DisplayName("getTopicsByDeckSlug")
    class GetTopicsByDeckSlugTests {

        @Test
        @DisplayName("Get topics by deck slug successfully: maps topics with user progress")
        void getTopicsByDeckSlug_Success() {
            Topic topic = Topic.builder().id(20L).name("Topic 1").build();
            TopicResponse topicResponse = new TopicResponse(20L, "Topic 1", "topic-1", 1, 10, 3);

            when(deckRepository.findAccessViewBySlug("my-deck")).thenReturn(Optional.of(accessView(DECK_ID)));
            when(topicRepository.findByDeckId(eq(DECK_ID), any(Sort.class))).thenReturn(List.of(topic));
            when(progressService.getLearnedCardCountByTopic(USER_ID, List.of(20L)))
                    .thenReturn(Map.of(20L, 3));
            when(topicMapper.toTopicResponse(topic, 3)).thenReturn(topicResponse);

            List<TopicResponse> result = deckService.getTopicsByDeckSlug("my-deck", USER_ID);

            assertThat(result).containsExactly(topicResponse);
            verify(accessPolicy).assertCanLearn(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Get topics by deck slug failed: slug not found")
        void getTopicsByDeckSlug_Fail_SlugNotFound() {
            when(deckRepository.findAccessViewBySlug("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.getTopicsByDeckSlug("unknown", USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verifyNoInteractions(accessPolicy, topicRepository);
        }

        @Test
        @DisplayName("Get topics by deck slug failed: access denied")
        void getTopicsByDeckSlug_Fail_AccessDenied() {
            when(deckRepository.findAccessViewBySlug("my-deck")).thenReturn(Optional.of(accessView(DECK_ID)));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> deckService.getTopicsByDeckSlug("my-deck", USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));

            verifyNoInteractions(topicRepository);
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

            when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(owner));
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
            when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.createUserDeck(USER_ID, new UserDeckRequest("My Deck", null)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));

            verify(deckRepository, never()).save(any(Deck.class));
        }

        @Test
        @DisplayName("Create user deck failed: deck limit reached")
        void createUserDeck_Fail_DeckLimitExceeded() {
            User owner = User.builder().id(USER_ID).username("tester").build();
            when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(owner));
            when(deckRepository.countByOwnerTypeAndOwnerId(OwnerType.USER, USER_ID))
                    .thenReturn((long) MAX_DECKS_PER_USER);

            assertThatThrownBy(() -> deckService.createUserDeck(USER_ID, new UserDeckRequest("My Deck", null)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_LIMIT_EXCEEDED));

            verify(deckRepository, never()).save(any(Deck.class));
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
            Deck deck = userDeck("My Deck");
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));

            deckService.deleteUserDeck(USER_ID, DECK_ID);

            verify(deckRepository).delete(deck);
            verify(accessPolicy).assertCanEdit(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Delete user deck failed: deck access view not found")
        void deleteUserDeck_Fail_DeckNotFound_AccessViewMissing() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.deleteUserDeck(USER_ID, DECK_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verify(deckRepository, never()).delete(any(Deck.class));
        }

        @Test
        @DisplayName("Delete user deck failed: deck entity disappeared")
        void deleteUserDeck_Fail_EntityNotFound() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView(DECK_ID)));
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.deleteUserDeck(USER_ID, DECK_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verify(deckRepository, never()).delete(any(Deck.class));
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

            verify(deckRepository, never()).delete(any(Deck.class));
        }
    }

    // ─────────────────────── searchDecksForAdmin ───────────────────────

    @Nested
    @DisplayName("searchDecksForAdmin")
    class SearchDecksForAdminTests {

        @Test
        @DisplayName("Search decks for admin successfully")
        void searchDecksForAdmin_Success() {
            AdminDeckSearchRequest request = new AdminDeckSearchRequest("deck", OwnerType.SYSTEM, DeckStatus.PUBLISHED, false, Set.of(), Set.of());
            Pageable pageable = PageRequest.of(0, 20);
            Deck deck = systemDeck("Title");
            Page<Deck> page = new PageImpl<>(List.of(deck), pageable, 1);
            DeckResponse response = buildDeckResponse("Title");

            when(deckRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(deckMapper.toDeckResponse(deck)).thenReturn(response);

            PageResponse<DeckResponse> result = deckService.searchDecksForAdmin(request, pageable);

            assertThat(result.content()).containsExactly(response);
            assertThat(result.totalElements()).isEqualTo(1L);
        }
    }

    // ─────────────────────── getDeckByIdForAdmin ───────────────────────

    @Nested
    @DisplayName("getDeckByIdForAdmin")
    class GetDeckByIdForAdminTests {

        @Test
        @DisplayName("Get deck by ID for admin successfully")
        void getDeckByIdForAdmin_Success() {
            Deck deck = systemDeck("Title");
            DeckResponse response = buildDeckResponse("Title");

            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));
            when(deckMapper.toDeckResponse(deck)).thenReturn(response);

            DeckResponse result = deckService.getDeckByIdForAdmin(DECK_ID);

            assertThat(result).isSameAs(response);
            verify(deckRepository).findById(DECK_ID);
        }

        @Test
        @DisplayName("Get deck by ID for admin failed: deck not found")
        void getDeckByIdForAdmin_Fail_DeckNotFound() {
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.getDeckByIdForAdmin(DECK_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));
        }
    }

    // ─────────────────────── createSystemDeck ───────────────────────

    @Nested
    @DisplayName("createSystemDeck")
    class CreateSystemDeckTests {

        @Test
        @DisplayName("Create system deck successfully with media, tags, and cefr levels")
        void createSystemDeck_Success_WithMediaTagsAndCefr() {
            SystemDeckRequest request = new SystemDeckRequest("System Deck", "desc", "verified/cover.png", true, Set.of(1L), Set.of(2L));
            Tag tag = Tag.builder().id(1L).build();
            CefrLevel cefr = CefrLevel.builder().id(2L).build();
            MediaKeyResolver.Resolved coverResolved = new MediaKeyResolver.Resolved("system/decks/cover.png", "verified/cover.png", List.of());
            Deck mappedDeck = new Deck();
            Deck savedDeck = systemDeck("System Deck");
            DeckResponse response = buildDeckResponse("System Deck");

            when(mediaKeyResolver.forCreate("verified/cover.png", SystemFileModule.DECKS)).thenReturn(coverResolved);
            when(deckMapper.toDeck(request)).thenReturn(mappedDeck);
            when(tagRepository.findAllById(Set.of(1L))).thenReturn(List.of(tag));
            when(cefrLevelRepository.findAllById(Set.of(2L))).thenReturn(List.of(cefr));
            when(deckRepository.save(any(Deck.class))).thenReturn(savedDeck);
            when(deckMapper.toDeckResponse(savedDeck)).thenReturn(response);

            DeckResponse result = deckService.createSystemDeck(request);

            assertThat(result).isSameAs(response);
            ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
            verify(deckRepository).save(captor.capture());
            Deck persisted = captor.getValue();
            assertThat(persisted.getCoverImageUrl()).isEqualTo("system/decks/cover.png");
            assertThat(persisted.getStatus()).isEqualTo(DeckStatus.DRAFT);
            assertThat(persisted.getOwnerType()).isEqualTo(OwnerType.SYSTEM);
            assertThat(persisted.getOwner()).isNull();
            assertThat(persisted.getTopicCount()).isZero();
            assertThat(persisted.getCardCount()).isZero();
            assertThat(persisted.getTags()).containsExactly(tag);
            assertThat(persisted.getCefrLevels()).containsExactly(cefr);
            verify(mediaKeyResolver).publish(coverResolved);
        }

        @Test
        @DisplayName("Create system deck successfully without media, tags, and cefr levels")
        void createSystemDeck_Success_WithoutMediaTagsAndCefr() {
            SystemDeckRequest request = new SystemDeckRequest("System Deck", null, null, false, null, null);
            MediaKeyResolver.Resolved emptyCover = new MediaKeyResolver.Resolved(null, null, List.of());
            Deck mappedDeck = new Deck();
            Deck savedDeck = systemDeck("System Deck");
            DeckResponse response = buildDeckResponse("System Deck");

            when(mediaKeyResolver.forCreate(null, SystemFileModule.DECKS)).thenReturn(emptyCover);
            when(deckMapper.toDeck(request)).thenReturn(mappedDeck);
            when(deckRepository.save(any(Deck.class))).thenReturn(savedDeck);
            when(deckMapper.toDeckResponse(savedDeck)).thenReturn(response);

            DeckResponse result = deckService.createSystemDeck(request);

            assertThat(result).isSameAs(response);
            verify(deckRepository).save(any(Deck.class));
            verify(mediaKeyResolver).publish(emptyCover);
            verify(tagRepository, never()).findAllById(any());
            verify(cefrLevelRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("Create system deck failed: tag not found")
        void createSystemDeck_Fail_TagNotFound() {
            SystemDeckRequest request = new SystemDeckRequest("System Deck", "desc", null, false, Set.of(1L, 2L), null);
            MediaKeyResolver.Resolved emptyCover = new MediaKeyResolver.Resolved(null, null, List.of());
            Tag tag1 = Tag.builder().id(1L).build();

            when(mediaKeyResolver.forCreate(null, SystemFileModule.DECKS)).thenReturn(emptyCover);
            when(deckMapper.toDeck(request)).thenReturn(new Deck());
            when(tagRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(tag1));

            assertThatThrownBy(() -> deckService.createSystemDeck(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.TAG_NOT_FOUND));

            verify(deckRepository, never()).save(any(Deck.class));
        }

        @Test
        @DisplayName("Create system deck failed: CEFR level not found")
        void createSystemDeck_Fail_CefrLevelNotFound() {
            SystemDeckRequest request = new SystemDeckRequest("System Deck", "desc", null, false, null, Set.of(1L, 2L));
            MediaKeyResolver.Resolved emptyCover = new MediaKeyResolver.Resolved(null, null, List.of());
            CefrLevel cefr1 = CefrLevel.builder().id(1L).build();

            when(mediaKeyResolver.forCreate(null, SystemFileModule.DECKS)).thenReturn(emptyCover);
            when(deckMapper.toDeck(request)).thenReturn(new Deck());
            when(cefrLevelRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(cefr1));

            assertThatThrownBy(() -> deckService.createSystemDeck(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CEFR_LEVEL_NOT_FOUND));

            verify(deckRepository, never()).save(any(Deck.class));
        }
    }

    // ─────────────────────── updateSystemDeck ───────────────────────

    @Nested
    @DisplayName("updateSystemDeck")
    class UpdateSystemDeckTests {

        @Test
        @DisplayName("Update system deck successfully")
        void updateSystemDeck_Success() {
            SystemDeckRequest request = new SystemDeckRequest("New Title", "desc", "verified/new_cover.png", false, Set.of(1L), Set.of());
            Deck existingDeck = systemDeck("Old Title");
            existingDeck.setCoverImageUrl("uploads/old_cover.png");
            Tag tag = Tag.builder().id(1L).build();
            MediaKeyResolver.Resolved coverResolved = new MediaKeyResolver.Resolved("system/decks/new_cover.png", "verified/new_cover.png", List.of("uploads/old_cover.png"));
            DeckResponse response = buildDeckResponse("New Title");

            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(existingDeck));
            when(mediaKeyResolver.forUpdate("verified/new_cover.png", "uploads/old_cover.png", SystemFileModule.DECKS)).thenReturn(coverResolved);
            when(tagRepository.findAllById(Set.of(1L))).thenReturn(List.of(tag));
            when(deckMapper.toDeckResponse(existingDeck)).thenReturn(response);

            DeckResponse result = deckService.updateSystemDeck(DECK_ID, request);

            assertThat(result).isSameAs(response);
            verify(deckMapper).updateSystemDeck(existingDeck, request);
            assertThat(existingDeck.getCoverImageUrl()).isEqualTo("system/decks/new_cover.png");
            assertThat(existingDeck.getTags()).containsExactly(tag);
            verify(mediaKeyResolver).publish(coverResolved);
        }

        @Test
        @DisplayName("Update system deck failed: deck not found")
        void updateSystemDeck_Fail_DeckNotFound() {
            SystemDeckRequest request = new SystemDeckRequest("New Title", null, null, false, null, null);
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.updateSystemDeck(DECK_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_FOUND));

            verify(deckMapper, never()).updateSystemDeck(any(), any());
        }

        @Test
        @DisplayName("Update system deck failed: deck is not system owned")
        void updateSystemDeck_Fail_DeckNotSystem() {
            SystemDeckRequest request = new SystemDeckRequest("New Title", null, null, false, null, null);
            Deck userOwnedDeck = userDeck("User Deck");
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(userOwnedDeck));

            assertThatThrownBy(() -> deckService.updateSystemDeck(DECK_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_SYSTEM));

            verify(deckMapper, never()).updateSystemDeck(any(), any());
        }

        @Test
        @DisplayName("Update system deck failed: tag not found")
        void updateSystemDeck_Fail_TagNotFound() {
            SystemDeckRequest request = new SystemDeckRequest("New Title", null, null, false, Set.of(1L, 2L), null);
            Deck existingDeck = systemDeck("Title");
            MediaKeyResolver.Resolved emptyCover = new MediaKeyResolver.Resolved(null, null, List.of());

            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(existingDeck));
            when(mediaKeyResolver.forUpdate(null, null, SystemFileModule.DECKS)).thenReturn(emptyCover);
            when(tagRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of());

            assertThatThrownBy(() -> deckService.updateSystemDeck(DECK_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.TAG_NOT_FOUND));
        }

        @Test
        @DisplayName("Update system deck failed: CEFR level not found")
        void updateSystemDeck_Fail_CefrLevelNotFound() {
            SystemDeckRequest request = new SystemDeckRequest("New Title", null, null, false, null, Set.of(1L, 2L));
            Deck existingDeck = systemDeck("Title");
            MediaKeyResolver.Resolved emptyCover = new MediaKeyResolver.Resolved(null, null, List.of());

            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(existingDeck));
            when(mediaKeyResolver.forUpdate(null, null, SystemFileModule.DECKS)).thenReturn(emptyCover);
            when(cefrLevelRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of());

            assertThatThrownBy(() -> deckService.updateSystemDeck(DECK_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CEFR_LEVEL_NOT_FOUND));
        }
    }

    // ─────────────────────── updateDeckStatus ───────────────────────

    @Nested
    @DisplayName("updateDeckStatus")
    class UpdateDeckStatusTests {

        @Test
        @DisplayName("Update status to PUBLISHED successfully with cards")
        void updateDeckStatus_Publish_Success() {
            Deck deck = systemDeck("System Deck");
            deck.setCardCount(5);
            deck.setStatus(DeckStatus.DRAFT);
            UpdateDeckStatusRequest request = new UpdateDeckStatusRequest(DeckStatus.PUBLISHED);
            DeckResponse response = buildDeckResponse("System Deck");

            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));
            when(deckMapper.toDeckResponse(deck)).thenReturn(response);

            DeckResponse result = deckService.updateDeckStatus(DECK_ID, request);

            assertThat(result).isSameAs(response);
            assertThat(deck.getStatus()).isEqualTo(DeckStatus.PUBLISHED);
            assertThat(deck.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Update status to PUBLISHED successfully when already published: keeps publishedAt")
        void updateDeckStatus_Publish_AlreadyPublished() {
            LocalDateTime publishedAt = LocalDateTime.now().minusDays(2);
            Deck deck = systemDeck("System Deck");
            deck.setCardCount(5);
            deck.setStatus(DeckStatus.PUBLISHED);
            deck.setPublishedAt(publishedAt);
            UpdateDeckStatusRequest request = new UpdateDeckStatusRequest(DeckStatus.PUBLISHED);

            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));
            when(deckMapper.toDeckResponse(deck)).thenReturn(buildDeckResponse("System Deck"));

            deckService.updateDeckStatus(DECK_ID, request);

            assertThat(deck.getPublishedAt()).isEqualTo(publishedAt);
        }

        @Test
        @DisplayName("Update status to PUBLISHED failed: deck has no cards")
        void updateDeckStatus_Publish_Fail_DeckEmpty() {
            Deck deck = systemDeck("System Deck");
            deck.setCardCount(0);
            UpdateDeckStatusRequest request = new UpdateDeckStatusRequest(DeckStatus.PUBLISHED);

            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));

            assertThatThrownBy(() -> deckService.updateDeckStatus(DECK_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_EMPTY));
        }

        @Test
        @DisplayName("Update status to ARCHIVED successfully")
        void updateDeckStatus_Archive_Success() {
            Deck deck = systemDeck("System Deck");
            deck.setStatus(DeckStatus.PUBLISHED);
            UpdateDeckStatusRequest request = new UpdateDeckStatusRequest(DeckStatus.ARCHIVED);

            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));
            when(deckMapper.toDeckResponse(deck)).thenReturn(buildDeckResponse("System Deck"));

            deckService.updateDeckStatus(DECK_ID, request);

            assertThat(deck.getStatus()).isEqualTo(DeckStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Update status to DRAFT successfully: clears publishedAt")
        void updateDeckStatus_Draft_Success() {
            Deck deck = systemDeck("System Deck");
            deck.setStatus(DeckStatus.PUBLISHED);
            deck.setPublishedAt(LocalDateTime.now());
            UpdateDeckStatusRequest request = new UpdateDeckStatusRequest(DeckStatus.DRAFT);

            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(deck));
            when(deckMapper.toDeckResponse(deck)).thenReturn(buildDeckResponse("System Deck"));

            deckService.updateDeckStatus(DECK_ID, request);

            assertThat(deck.getStatus()).isEqualTo(DeckStatus.DRAFT);
            assertThat(deck.getPublishedAt()).isNull();
        }

        @Test
        @DisplayName("Update status failed: deck not found")
        void updateDeckStatus_Fail_DeckNotFound() {
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.updateDeckStatus(DECK_ID, new UpdateDeckStatusRequest(DeckStatus.PUBLISHED)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_FOUND));
        }

        @Test
        @DisplayName("Update status failed: deck is not system owned")
        void updateDeckStatus_Fail_DeckNotSystem() {
            Deck userOwnedDeck = userDeck("User Deck");
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(userOwnedDeck));

            assertThatThrownBy(() -> deckService.updateDeckStatus(DECK_ID, new UpdateDeckStatusRequest(DeckStatus.PUBLISHED)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_SYSTEM));
        }
    }

    // ─────────────────────── deleteUserDeckByAdmin ───────────────────────

    @Nested
    @DisplayName("deleteUserDeckByAdmin")
    class DeleteUserDeckByAdminTests {

        @Test
        @DisplayName("Delete user deck by admin successfully")
        void deleteUserDeckByAdmin_Success() {
            Deck userOwnedDeck = userDeck("User Deck");
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(userOwnedDeck));

            deckService.deleteUserDeckByAdmin(DECK_ID, "Policy violation");

            verify(deckRepository).delete(userOwnedDeck);
        }

        @Test
        @DisplayName("Delete user deck by admin failed: deck not found")
        void deleteUserDeckByAdmin_Fail_DeckNotFound() {
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckService.deleteUserDeckByAdmin(DECK_ID, "Violation"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_FOUND));

            verify(deckRepository, never()).delete(any(Deck.class));
        }

        @Test
        @DisplayName("Delete user deck by admin failed: cannot delete system deck")
        void deleteUserDeckByAdmin_Fail_CannotDeleteSystemDeck() {
            Deck sysDeck = systemDeck("System Deck");
            when(deckRepository.findById(DECK_ID)).thenReturn(Optional.of(sysDeck));

            assertThatThrownBy(() -> deckService.deleteUserDeckByAdmin(DECK_ID, "Violation"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_DELETE_SYSTEM_DECK));

            verify(deckRepository, never()).delete(any(Deck.class));
        }
    }
}
