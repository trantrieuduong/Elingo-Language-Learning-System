package com.elingo.vocabulary.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.vocabulary.config.FlashcardProperties;
import com.elingo.vocabulary.dto.request.CreateUserCardRequest;
import com.elingo.vocabulary.dto.request.UpdateUserCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.impl.CardServiceImpl;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CardServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private FlashcardProperties flashcardProperties;

    @Mock
    private DeckAccessPolicy accessPolicy;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardServiceImpl cardService;

    private static final Long USER_ID = 1L;
    private static final Long DECK_ID = 10L;
    private static final Long TOPIC_ID = 20L;
    private static final Long CARD_ID = 100L;
    private static final int MAX_CARDS_PER_TOPIC = 500;

    private Topic topic;
    private Deck deck;
    private Card card;

    @BeforeEach
    void setUp() {
        FlashcardProperties.UserDeckLimit limits = new FlashcardProperties.UserDeckLimit();
        limits.setMaxCardsPerTopic(MAX_CARDS_PER_TOPIC);
        lenient().when(flashcardProperties.getUserDeckLimit()).thenReturn(limits);

        deck = Deck.builder().id(DECK_ID).build();
        topic = Topic.builder().id(TOPIC_ID).deck(deck).name("Animals").build();
        card = Card.builder().id(CARD_ID).topic(topic).deck(deck).term("cat").order(1).build();
    }

    private DeckRepository.DeckAccessView accessView() {
        return TestDeckAccessView.published(DECK_ID);
    }

    private CreateUserCardRequest createRequest() {
        return new CreateUserCardRequest(TOPIC_ID, "cat", "n", "con meo", null, null, null, null);
    }

    private UpdateUserCardRequest updateRequest() {
        return new UpdateUserCardRequest("dog", "n", "con cho", null, null, null, null);
    }

    private CardResponse cardResponse(String term) {
        return new CardResponse(CARD_ID, 1, term, "n", "nghia", null, null, null, null,
                null, List.of(), null);
    }

    // ─────────────────────── createUserCard ───────────────────────

    @Nested
    @DisplayName("createUserCard")
    class CreateUserCardTests {

        @Test
        @DisplayName("Create user card successfully: order is the next one after the last card")
        void createUserCard_Success_NextOrder() {
            Card lastCard = Card.builder().id(99L).order(7).build();
            Card savedCard = Card.builder().id(CARD_ID).topic(topic).deck(deck).term("cat").order(8).build();

            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.countByTopicId(TOPIC_ID)).thenReturn(7L);
            when(cardRepository.findFirstByTopicIdOrderByOrderDesc(TOPIC_ID)).thenReturn(Optional.of(lastCard));
            when(cardMapper.toCard(any(CreateUserCardRequest.class))).thenReturn(new Card());
            when(deckRepository.getReferenceById(DECK_ID)).thenReturn(deck);
            when(cardRepository.save(any(Card.class))).thenReturn(savedCard);
            when(cardMapper.toCardResponse(savedCard)).thenReturn(cardResponse("cat"));

            CardResponse result = cardService.createUserCard(USER_ID, createRequest());

            assertThat(result.term()).isEqualTo("cat");
            ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
            verify(cardRepository).save(captor.capture());
            Card persisted = captor.getValue();
            assertThat(persisted.getOrder()).isEqualTo(8);
            assertThat(persisted.getDeck()).isSameAs(deck);
            assertThat(persisted.getTopic()).isSameAs(topic);
            assertThat(persisted.getPhonetics()).isEmpty();
            verify(topicRepository).adjustCardCount(TOPIC_ID, 1);
            verify(deckRepository).adjustCardCount(DECK_ID, 1);
            verify(accessPolicy).assertCanEdit(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Create user card successfully: order starts at 1 for an empty topic")
        void createUserCard_Success_FirstOrder() {
            Card savedCard = Card.builder().id(CARD_ID).order(1).build();

            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.countByTopicId(TOPIC_ID)).thenReturn(0L);
            when(cardRepository.findFirstByTopicIdOrderByOrderDesc(TOPIC_ID)).thenReturn(Optional.empty());
            when(cardMapper.toCard(any(CreateUserCardRequest.class))).thenReturn(new Card());
            when(deckRepository.getReferenceById(DECK_ID)).thenReturn(deck);
            when(cardRepository.save(any(Card.class))).thenReturn(savedCard);
            when(cardMapper.toCardResponse(savedCard)).thenReturn(cardResponse("cat"));

            cardService.createUserCard(USER_ID, createRequest());

            ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
            verify(cardRepository).save(captor.capture());
            assertThat(captor.getValue().getOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("Create user card failed: topic not found")
        void createUserCard_Fail_TopicNotFound() {
            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.createUserCard(USER_ID, createRequest()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.TOPIC_NOT_FOUND));

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create user card failed: deck of the topic not found")
        void createUserCard_Fail_DeckNotFound() {
            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.createUserCard(USER_ID, createRequest()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.TOPIC_NOT_FOUND));

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create user card failed: deck access denied")
        void createUserCard_Fail_AccessDenied() {
            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(accessPolicy).assertCanEdit(any(), eq(USER_ID));

            assertThatThrownBy(() -> cardService.createUserCard(USER_ID, createRequest()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));

            verify(cardRepository, never()).save(any());
            verify(topicRepository, never()).adjustCardCount(any(), anyInt());
        }

        @Test
        @DisplayName("Create user card failed: card limit reached")
        void createUserCard_Fail_CardLimitExceeded() {
            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.countByTopicId(TOPIC_ID)).thenReturn((long) MAX_CARDS_PER_TOPIC);

            assertThatThrownBy(() -> cardService.createUserCard(USER_ID, createRequest()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.CARD_LIMIT_EXCEEDED));

            verify(cardRepository, never()).save(any());
            verify(topicRepository, never()).adjustCardCount(any(), anyInt());
        }
    }

    // ─────────────────────── updateUserCard ───────────────────────

    @Nested
    @DisplayName("updateUserCard")
    class UpdateUserCardTests {

        @Test
        @DisplayName("Update user card successfully")
        void updateUserCard_Success() {
            when(deckRepository.findAccessViewByCardId(CARD_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
            when(cardMapper.toCardResponse(card)).thenReturn(cardResponse("dog"));

            CardResponse result = cardService.updateUserCard(USER_ID, CARD_ID, updateRequest());

            assertThat(result.term()).isEqualTo("dog");
            verify(cardMapper).updateCard(card, updateRequest());
            verify(accessPolicy).assertCanEdit(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Update user card failed: card not found")
        void updateUserCard_Fail_CardNotFound() {
            when(deckRepository.findAccessViewByCardId(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.updateUserCard(USER_ID, CARD_ID, updateRequest()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.CARD_NOT_FOUND));

            verify(cardMapper, never()).updateCard(any(), any());
        }

        @Test
        @DisplayName("Update user card failed: card exists but the entity is gone")
        void updateUserCard_Fail_EntityNotFound() {
            when(deckRepository.findAccessViewByCardId(CARD_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.updateUserCard(USER_ID, CARD_ID, updateRequest()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.CARD_NOT_FOUND));

            verify(cardMapper, never()).updateCard(any(), any());
        }

        @Test
        @DisplayName("Update user card failed: deck access denied")
        void updateUserCard_Fail_AccessDenied() {
            when(deckRepository.findAccessViewByCardId(CARD_ID)).thenReturn(Optional.of(accessView()));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(accessPolicy).assertCanEdit(any(), eq(USER_ID));

            assertThatThrownBy(() -> cardService.updateUserCard(USER_ID, CARD_ID, updateRequest()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));

            verify(cardRepository, never()).findById(any());
            verify(cardMapper, never()).updateCard(any(), any());
        }
    }

    // ─────────────────────── deleteUserCard ───────────────────────

    @Nested
    @DisplayName("deleteUserCard")
    class DeleteUserCardTests {

        @Test
        @DisplayName("Delete user card successfully: counters are decremented")
        void deleteUserCard_Success() {
            when(deckRepository.findAccessViewByCardId(CARD_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

            cardService.deleteUserCard(USER_ID, CARD_ID);

            verify(cardRepository).delete(card);
            verify(topicRepository).adjustCardCount(TOPIC_ID, -1);
            verify(deckRepository).adjustCardCount(DECK_ID, -1);
            verify(accessPolicy).assertCanEdit(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("Delete user card failed: card not found")
        void deleteUserCard_Fail_CardNotFound() {
            when(deckRepository.findAccessViewByCardId(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.deleteUserCard(USER_ID, CARD_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.CARD_NOT_FOUND));

            verify(cardRepository, never()).delete(any());
            verify(deckRepository, never()).adjustCardCount(any(), anyInt());
        }

        @Test
        @DisplayName("Delete user card failed: deck access denied")
        void deleteUserCard_Fail_AccessDenied() {
            when(deckRepository.findAccessViewByCardId(CARD_ID)).thenReturn(Optional.of(accessView()));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(accessPolicy).assertCanEdit(any(), eq(USER_ID));

            assertThatThrownBy(() -> cardService.deleteUserCard(USER_ID, CARD_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));

            verify(cardRepository, never()).delete(any());
        }
    }
}
