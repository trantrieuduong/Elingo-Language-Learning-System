package com.elingo.vocabulary.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.flashcard.dto.response.CardLearningState;
import com.elingo.learning.flashcard.service.CardStateService;
import com.elingo.vocabulary.config.FlashcardProperties;
import com.elingo.vocabulary.dto.request.CreateUserTopicRequest;
import com.elingo.vocabulary.dto.request.UpdateUserTopicRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.impl.TopicServiceImpl;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class TopicServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private DeckAccessPolicy accessPolicy;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardStateService cardStateService;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private FlashcardProperties flashcardProperties;

    @Mock
    private TopicMapper topicMapper;

    @InjectMocks
    private TopicServiceImpl topicService;

    private static final Long USER_ID = 1L;
    private static final Long DECK_ID = 10L;
    private static final Long TOPIC_ID = 20L;
    private static final int MAX_TOPICS_PER_DECK = 50;

    private Deck deck;
    private Topic topic;

    @BeforeEach
    void setUp() {
        FlashcardProperties.UserDeckLimit limits = new FlashcardProperties.UserDeckLimit();
        limits.setMaxTopicsPerDeck(MAX_TOPICS_PER_DECK);
        lenient().when(flashcardProperties.getUserDeckLimit()).thenReturn(limits);

        deck = Deck.builder().id(DECK_ID).build();
        topic = Topic.builder().id(TOPIC_ID).deck(deck).name("Animals").slug("animals").order(1).build();
    }

    private DeckRepository.DeckAccessView accessView() {
        return TestDeckAccessView.published(DECK_ID);
    }

    private Card buildCard(Long id, int order, String term) {
        return Card.builder().id(id).topic(topic).deck(deck).term(term).order(order).build();
    }

    private CardResponse cardResponse(String term, CardLearningState state) {
        return new CardResponse(1L, 1, term, "n", "nghia", null, null, null, null,
                null, List.of(), state);
    }

    // ─────────────────────── getCardsByTopic ───────────────────────

    @Nested
    @DisplayName("getCardsByTopic")
    class GetCardsByTopicTests {

        @Test
        @DisplayName("Get cards by topic successfully: attaches the learning state of the user")
        void getCardsByTopic_Success_WithState() {
            Card card1 = buildCard(1L, 1, "cat");
            Card card2 = buildCard(2L, 2, "dog");
            CardLearningState learned = new CardLearningState(true, true, false);
            CardResponse response1 = cardResponse("cat", learned);
            CardResponse response2 = cardResponse("dog", CardLearningState.NONE);

            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.findByTopicIdWithPhonetics(TOPIC_ID)).thenReturn(List.of(card1, card2));
            when(cardStateService.getStates(USER_ID, List.of(1L, 2L))).thenReturn(Map.of(1L, learned));
            when(cardMapper.toCardResponse(card1, learned)).thenReturn(response1);
            when(cardMapper.toCardResponse(card2, CardLearningState.NONE)).thenReturn(response2);

            List<CardResponse> result = topicService.getCardsByTopic(TOPIC_ID, USER_ID);

            assertThat(result).containsExactly(response1, response2);
            verify(accessPolicy).assertCanLearn(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Get cards by topic successfully: anonymous user skips the state lookup")
        void getCardsByTopic_Success_Anonymous() {
            Card card = buildCard(1L, 1, "cat");
            CardResponse response = cardResponse("cat", null);

            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.findByTopicIdWithPhonetics(TOPIC_ID)).thenReturn(List.of(card));
            when(cardMapper.toCardResponse(card, null)).thenReturn(response);

            List<CardResponse> result = topicService.getCardsByTopic(TOPIC_ID, null);

            assertThat(result).containsExactly(response);
            verifyNoInteractions(cardStateService);
        }

        @Test
        @DisplayName("Get cards by topic successfully: empty topic skips the state lookup")
        void getCardsByTopic_Success_EmptyTopic() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.findByTopicIdWithPhonetics(TOPIC_ID)).thenReturn(List.of());

            List<CardResponse> result = topicService.getCardsByTopic(TOPIC_ID, USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(cardStateService);
        }

        @Test
        @DisplayName("Get cards by topic failed: topic not found")
        void getCardsByTopic_Fail_TopicNotFound() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> topicService.getCardsByTopic(TOPIC_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.TOPIC_NOT_FOUND));

            verify(cardRepository, never()).findByTopicIdWithPhonetics(any());
        }

        @Test
        @DisplayName("Get cards by topic failed: deck is not accessible")
        void getCardsByTopic_Fail_DeckNotAccessible() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            doThrow(new AppException(AppError.DECK_NOT_FOUND))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> topicService.getCardsByTopic(TOPIC_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verify(cardRepository, never()).findByTopicIdWithPhonetics(any());
        }

        @Test
        @DisplayName("Get cards by topic failed: premium deck without subscription")
        void getCardsByTopic_Fail_PremiumRequired() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            doThrow(new AppException(AppError.PREMIUM_REQUIRED))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> topicService.getCardsByTopic(TOPIC_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PREMIUM_REQUIRED));

            verify(cardRepository, never()).findByTopicIdWithPhonetics(any());
        }
    }

    // ─────────────────────── createUserTopic ───────────────────────

    @Nested
    @DisplayName("createUserTopic")
    class CreateUserTopicTests {

        @Test
        @DisplayName("Create user topic successfully: appends at the end of the deck")
        void createUserTopic_Success_NextOrder() {
            Topic lastTopic = Topic.builder().id(5L).order(3).build();
            TopicResponse response = new TopicResponse(TOPIC_ID, "Animals", "animals", 4, 0, null);

            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView()));
            when(topicRepository.countByDeckId(DECK_ID)).thenReturn(3L);
            when(topicRepository.findFirstByDeckIdOrderByOrderDesc(DECK_ID)).thenReturn(Optional.of(lastTopic));
            when(deckRepository.getReferenceById(DECK_ID)).thenReturn(deck);
            when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
                Topic saved = inv.getArgument(0);
                saved.setId(TOPIC_ID);
                return saved;
            });
            when(topicMapper.toTopicResponse(any(Topic.class))).thenReturn(response);

            TopicResponse result = topicService.createUserTopic(USER_ID,
                    new CreateUserTopicRequest(DECK_ID, "Animals"));

            assertThat(result).isSameAs(response);
            ArgumentCaptor<Topic> captor = ArgumentCaptor.forClass(Topic.class);
            verify(topicRepository).save(captor.capture());
            Topic persisted = captor.getValue();
            assertThat(persisted.getName()).isEqualTo("Animals");
            assertThat(persisted.getOrder()).isEqualTo(4);
            assertThat(persisted.getDeck()).isSameAs(deck);
            assertThat(persisted.getSlug()).isNotBlank();
            verify(deckRepository).adjustTopicCount(DECK_ID, 1);
            verify(accessPolicy).assertCanEdit(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Create user topic successfully: first topic starts at order 1")
        void createUserTopic_Success_FirstOrder() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView()));
            when(topicRepository.countByDeckId(DECK_ID)).thenReturn(0L);
            when(topicRepository.findFirstByDeckIdOrderByOrderDesc(DECK_ID)).thenReturn(Optional.empty());
            when(deckRepository.getReferenceById(DECK_ID)).thenReturn(deck);
            when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> inv.getArgument(0));
            when(topicMapper.toTopicResponse(any(Topic.class)))
                    .thenReturn(new TopicResponse(TOPIC_ID, "Animals", "animals", 1, 0, null));

            topicService.createUserTopic(USER_ID, new CreateUserTopicRequest(DECK_ID, "Animals"));

            ArgumentCaptor<Topic> captor = ArgumentCaptor.forClass(Topic.class);
            verify(topicRepository).save(captor.capture());
            assertThat(captor.getValue().getOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("Create user topic failed: deck not found")
        void createUserTopic_Fail_DeckNotFound() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> topicService.createUserTopic(USER_ID,
                    new CreateUserTopicRequest(DECK_ID, "Animals")))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verify(topicRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create user topic failed: user does not own the deck")
        void createUserTopic_Fail_AccessDenied() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView()));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(accessPolicy).assertCanEdit(any(), eq(USER_ID));

            assertThatThrownBy(() -> topicService.createUserTopic(USER_ID,
                    new CreateUserTopicRequest(DECK_ID, "Animals")))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));

            verify(topicRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create user topic failed: topic limit reached")
        void createUserTopic_Fail_TopicLimitExceeded() {
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(accessView()));
            when(topicRepository.countByDeckId(DECK_ID)).thenReturn((long) MAX_TOPICS_PER_DECK);

            assertThatThrownBy(() -> topicService.createUserTopic(USER_ID,
                    new CreateUserTopicRequest(DECK_ID, "Animals")))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.TOPIC_LIMIT_EXCEEDED));

            verify(topicRepository, never()).save(any());
            verify(deckRepository, never()).adjustTopicCount(any(), anyInt());
        }
    }

    // ─────────────────────── updateUserTopic ───────────────────────

    @Nested
    @DisplayName("updateUserTopic")
    class UpdateUserTopicTests {

        @Test
        @DisplayName("Update user topic successfully")
        void updateUserTopic_Success() {
            UpdateUserTopicRequest request = new UpdateUserTopicRequest("Animals");
            TopicResponse response = new TopicResponse(TOPIC_ID, "Animals", "animals", 1, 0, null);

            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
            when(topicMapper.toTopicResponse(topic)).thenReturn(response);

            TopicResponse result = topicService.updateUserTopic(USER_ID, TOPIC_ID, request);

            assertThat(result).isSameAs(response);
            verify(topicMapper).update(topic, request);
            verify(accessPolicy).assertCanEdit(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Update user topic failed: topic not found")
        void updateUserTopic_Fail_TopicNotFound() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> topicService.updateUserTopic(USER_ID, TOPIC_ID,
                    new UpdateUserTopicRequest("Animals")))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.TOPIC_NOT_FOUND));

            verify(topicMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("Update user topic failed: topic entity disappeared")
        void updateUserTopic_Fail_EntityNotFound() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> topicService.updateUserTopic(USER_ID, TOPIC_ID,
                    new UpdateUserTopicRequest("Animals")))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.TOPIC_NOT_FOUND));

            verify(topicMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("Update user topic failed: user does not own the deck")
        void updateUserTopic_Fail_AccessDenied() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(accessPolicy).assertCanEdit(any(), eq(USER_ID));

            assertThatThrownBy(() -> topicService.updateUserTopic(USER_ID, TOPIC_ID,
                    new UpdateUserTopicRequest("Animals")))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));

            verify(topicRepository, never()).findById(any());
        }
    }

    // ─────────────────────── deleteUserTopic ───────────────────────

    @Nested
    @DisplayName("deleteUserTopic")
    class DeleteUserTopicTests {

        @Test
        @DisplayName("Delete user topic successfully: counters drop by the detached card count")
        void deleteUserTopic_Success_WithDetachedCards() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.detachByTopicId(TOPIC_ID)).thenReturn(3);

            topicService.deleteUserTopic(USER_ID, TOPIC_ID);

            verify(topicRepository).deleteById(TOPIC_ID);
            verify(deckRepository).adjustTopicCount(DECK_ID, -1);
            verify(deckRepository).adjustCardCount(DECK_ID, -3);
        }

        @Test
        @DisplayName("Delete user topic successfully: no card counter update when topic is empty")
        void deleteUserTopic_Success_NoDetachedCards() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.detachByTopicId(TOPIC_ID)).thenReturn(0);

            topicService.deleteUserTopic(USER_ID, TOPIC_ID);

            verify(topicRepository).deleteById(TOPIC_ID);
            verify(deckRepository).adjustTopicCount(DECK_ID, -1);
            verify(deckRepository, never()).adjustCardCount(any(), anyInt());
        }

        @Test
        @DisplayName("Delete user topic failed: topic not found")
        void deleteUserTopic_Fail_TopicNotFound() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> topicService.deleteUserTopic(USER_ID, TOPIC_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.TOPIC_NOT_FOUND));

            verify(topicRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Delete user topic failed: user does not own the deck")
        void deleteUserTopic_Fail_AccessDenied() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(accessPolicy).assertCanEdit(any(), eq(USER_ID));

            assertThatThrownBy(() -> topicService.deleteUserTopic(USER_ID, TOPIC_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));

            verify(cardRepository, never()).detachByTopicId(any());
            verify(topicRepository, never()).deleteById(any());
        }
    }
}
