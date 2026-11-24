package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.request.CardPhoneticRequest;
import com.elingo.vocabulary.dto.request.CreateSystemCardRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemCardRequest;
import com.elingo.vocabulary.dto.response.OrphanedCardCleanupResponse;
import com.elingo.vocabulary.dto.response.OrphanedCardStatsResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.enums.SystemFileModule;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.file.util.MediaKeyResolver;
import com.elingo.vocabulary.config.FlashcardProperties;
import com.elingo.vocabulary.dto.request.CreateUserCardRequest;
import com.elingo.vocabulary.dto.request.UpdateUserCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.entity.CardPhonetic;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.mapper.CardPhoneticMapper;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
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
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    private CardPhoneticMapper cardPhoneticMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MediaKeyResolver mediaKeyResolver;

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

        deck = Deck.builder().id(DECK_ID).ownerType(OwnerType.USER).build();
        topic = Topic.builder().id(TOPIC_ID).deck(deck).name("Animals").build();
        card = Card.builder()
                .id(CARD_ID)
                .topic(topic)
                .deck(deck)
                .term("cat")
                .order(1)
                .phonetics(new ArrayList<>())
                .build();
    }

    private DeckRepository.DeckAccessView accessView() {
        return TestDeckAccessView.published(DECK_ID);
    }

    private Deck systemDeck() {
        return Deck.builder().id(DECK_ID).ownerType(OwnerType.SYSTEM).build();
    }

    private Topic systemTopic() {
        return Topic.builder().id(TOPIC_ID).deck(systemDeck()).name("Animals").build();
    }

    private CreateUserCardRequest createRequest() {
        return new CreateUserCardRequest(TOPIC_ID, "cat", "n", "con meo", null, null, null, null);
    }

    private UpdateUserCardRequest updateRequest() {
        return new UpdateUserCardRequest("dog", "n", "con cho", null, null, null, null);
    }

    private CreateSystemCardRequest createSystemCardRequest(String imageUrl, List<CardPhoneticRequest> phonetics) {
        return new CreateSystemCardRequest(TOPIC_ID, "cat", "n", "con meo", "vi", "en", "ex vi", "ex en", imageUrl, phonetics);
    }

    private UpdateSystemCardRequest updateSystemCardRequest(String imageUrl, List<CardPhoneticRequest> phonetics) {
        return new UpdateSystemCardRequest("dog", "n", "con cho", "vi", "en", "ex vi", "ex en", imageUrl, phonetics);
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

            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(topicRepository.findByIdForUpdate(TOPIC_ID)).thenReturn(Optional.of(topic));
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

            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(topicRepository.findByIdForUpdate(TOPIC_ID)).thenReturn(Optional.of(topic));
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
        @DisplayName("Create user card failed: topic not found because access view is missing")
        void createUserCard_Fail_TopicNotFound_AccessViewMissing() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.createUserCard(USER_ID, createRequest()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.TOPIC_NOT_FOUND));

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create user card failed: topic entity disappeared from database")
        void createUserCard_Fail_TopicNotFound_EntityDisappeared() {
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(topicRepository.findByIdForUpdate(TOPIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.createUserCard(USER_ID, createRequest()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.TOPIC_NOT_FOUND));

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create user card failed: deck access denied")
        void createUserCard_Fail_AccessDenied() {
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
            when(deckRepository.findAccessViewByTopicId(TOPIC_ID)).thenReturn(Optional.of(accessView()));
            when(topicRepository.findByIdForUpdate(TOPIC_ID)).thenReturn(Optional.of(topic));
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
        @DisplayName("Update user card failed: card not found because access view is missing")
        void updateUserCard_Fail_CardNotFound() {
            when(deckRepository.findAccessViewByCardId(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.updateUserCard(USER_ID, CARD_ID, updateRequest()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.CARD_NOT_FOUND));

            verify(cardMapper, never()).updateCard(any(), any());
        }

        @Test
        @DisplayName("Update user card failed: card exists in access view but entity is gone")
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
        @DisplayName("Delete user card successfully without media files: counters decremented and no events published")
        void deleteUserCard_Success_WithoutMediaFiles() {
            when(deckRepository.findAccessViewByCardId(CARD_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

            cardService.deleteUserCard(USER_ID, CARD_ID);

            verify(cardRepository).delete(card);
            verify(topicRepository).adjustCardCount(TOPIC_ID, -1);
            verify(deckRepository).adjustCardCount(DECK_ID, -1);
            verify(accessPolicy).assertCanEdit(any(), eq(USER_ID));
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Delete user card successfully with media files: counters decremented and FileDeletedEvent published")
        void deleteUserCard_Success_WithMediaFiles() {
            Card cardWithMedia = Card.builder()
                    .id(CARD_ID)
                    .topic(topic)
                    .deck(deck)
                    .term("cat")
                    .imageUrl("uploads/cards/cat.png")
                    .phonetics(List.of(
                            CardPhonetic.builder().audioUrl("uploads/phonetics/cat.mp3").build()
                    ))
                    .build();

            when(deckRepository.findAccessViewByCardId(CARD_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(cardWithMedia));

            cardService.deleteUserCard(USER_ID, CARD_ID);

            verify(cardRepository).delete(cardWithMedia);
            verify(topicRepository).adjustCardCount(TOPIC_ID, -1);
            verify(deckRepository).adjustCardCount(DECK_ID, -1);

            ArgumentCaptor<FileDeletedEvent> captor = ArgumentCaptor.forClass(FileDeletedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().fileKeys()).containsExactlyInAnyOrder("uploads/cards/cat.png", "uploads/phonetics/cat.mp3");
        }

        @Test
        @DisplayName("Delete user card failed: card not found because access view is missing")
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
        @DisplayName("Delete user card failed: card exists in access view but entity is missing")
        void deleteUserCard_Fail_EntityNotFound() {
            when(deckRepository.findAccessViewByCardId(CARD_ID)).thenReturn(Optional.of(accessView()));
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.deleteUserCard(USER_ID, CARD_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.CARD_NOT_FOUND));

            verify(cardRepository, never()).delete(any());
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

    // ─────────────────────── getCardsByTopicIdForAdmin ───────────────────────

    @Nested
    @DisplayName("getCardsByTopicIdForAdmin")
    class GetCardsByTopicIdForAdminTests {

        @Test
        @DisplayName("Get cards by topic ID for admin successfully")
        void getCardsByTopicIdForAdmin_Success() {
            CardResponse response = cardResponse("cat");

            when(topicRepository.existsById(TOPIC_ID)).thenReturn(true);
            when(cardRepository.findByTopicIdWithPhonetics(TOPIC_ID)).thenReturn(List.of(card));
            when(cardMapper.toCardResponse(card)).thenReturn(response);

            List<CardResponse> result = cardService.getCardsByTopicIdForAdmin(TOPIC_ID);

            assertThat(result).containsExactly(response);
            verify(topicRepository).existsById(TOPIC_ID);
            verify(cardRepository).findByTopicIdWithPhonetics(TOPIC_ID);
        }

        @Test
        @DisplayName("Get cards by topic ID for admin failed: topic not found")
        void getCardsByTopicIdForAdmin_Fail_TopicNotFound() {
            when(topicRepository.existsById(TOPIC_ID)).thenReturn(false);

            assertThatThrownBy(() -> cardService.getCardsByTopicIdForAdmin(TOPIC_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.TOPIC_NOT_FOUND));

            verify(cardRepository, never()).findByTopicIdWithPhonetics(any());
        }
    }

    // ─────────────────────── getCardByIdForAdmin ───────────────────────

    @Nested
    @DisplayName("getCardByIdForAdmin")
    class GetCardByIdForAdminTests {

        @Test
        @DisplayName("Get card by ID for admin successfully")
        void getCardByIdForAdmin_Success() {
            CardResponse response = cardResponse("cat");

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
            when(cardMapper.toCardResponse(card)).thenReturn(response);

            CardResponse result = cardService.getCardByIdForAdmin(CARD_ID);

            assertThat(result).isSameAs(response);
            verify(cardRepository).findById(CARD_ID);
        }

        @Test
        @DisplayName("Get card by ID for admin failed: card not found")
        void getCardByIdForAdmin_Fail_CardNotFound() {
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.getCardByIdForAdmin(CARD_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CARD_NOT_FOUND));
        }
    }

    // ─────────────────────── createSystemCard ───────────────────────

    @Nested
    @DisplayName("createSystemCard")
    class CreateSystemCardTests {

        @Test
        @DisplayName("Create system card successfully with image and phonetics")
        void createSystemCard_Success_WithMediaAndPhonetics() {
            Deck sysDeck = systemDeck();
            Topic sysTopic = Topic.builder().id(TOPIC_ID).deck(sysDeck).name("Animals").build();
            Card sysCard = Card.builder().id(CARD_ID).deck(sysDeck).topic(sysTopic).term("cat").order(1).build();

            CardPhoneticRequest phoneticReq = new CardPhoneticRequest("/kæt/", "verified/audio.mp3", "en-US");
            CreateSystemCardRequest req = createSystemCardRequest("verified/img.png", List.of(phoneticReq));

            MediaKeyResolver.Resolved resolvedImage = new MediaKeyResolver.Resolved("system/cards/img.png", "verified/img.png", List.of());
            MediaKeyResolver.Resolved resolvedAudio = new MediaKeyResolver.Resolved("system/phonetics/audio.mp3", "verified/audio.mp3", List.of());

            when(topicRepository.findByIdForUpdate(TOPIC_ID)).thenReturn(Optional.of(sysTopic));
            when(mediaKeyResolver.forCreate("verified/img.png", SystemFileModule.CARDS)).thenReturn(resolvedImage);
            when(mediaKeyResolver.forCreate("verified/audio.mp3", SystemFileModule.PHONETICS)).thenReturn(resolvedAudio);
            when(cardRepository.findFirstByTopicIdOrderByOrderDesc(TOPIC_ID)).thenReturn(Optional.empty());
            when(cardMapper.toCard(req)).thenReturn(new Card());
            when(cardPhoneticMapper.toCardPhonetic(phoneticReq)).thenReturn(new CardPhonetic());
            when(cardRepository.save(any(Card.class))).thenReturn(sysCard);
            when(cardMapper.toCardResponse(sysCard)).thenReturn(cardResponse("cat"));

            CardResponse result = cardService.createSystemCard(req);

            assertThat(result.term()).isEqualTo("cat");
            ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
            verify(cardRepository).save(captor.capture());
            Card persisted = captor.getValue();
            assertThat(persisted.getImageUrl()).isEqualTo("system/cards/img.png");
            assertThat(persisted.getDeck()).isSameAs(sysDeck);
            assertThat(persisted.getTopic()).isSameAs(sysTopic);
            assertThat(persisted.getOrder()).isEqualTo(1);
            assertThat(persisted.getPhonetics()).hasSize(1);
            assertThat(persisted.getPhonetics().get(0).getAudioUrl()).isEqualTo("system/phonetics/audio.mp3");

            verify(topicRepository).adjustCardCount(TOPIC_ID, 1);
            verify(deckRepository).adjustCardCount(DECK_ID, 1);
            verify(mediaKeyResolver).publish(resolvedImage);
            verify(mediaKeyResolver).publish(resolvedAudio);
        }

        @Test
        @DisplayName("Create system card successfully without media files")
        void createSystemCard_Success_WithoutMedia() {
            Deck sysDeck = systemDeck();
            Topic sysTopic = Topic.builder().id(TOPIC_ID).deck(sysDeck).name("Animals").build();
            Card sysCard = Card.builder().id(CARD_ID).deck(sysDeck).topic(sysTopic).term("cat").order(1).build();

            CreateSystemCardRequest req = createSystemCardRequest(null, null);
            MediaKeyResolver.Resolved emptyImage = new MediaKeyResolver.Resolved(null, null, List.of());

            when(topicRepository.findByIdForUpdate(TOPIC_ID)).thenReturn(Optional.of(sysTopic));
            when(mediaKeyResolver.forCreate(null, SystemFileModule.CARDS)).thenReturn(emptyImage);
            when(cardRepository.findFirstByTopicIdOrderByOrderDesc(TOPIC_ID)).thenReturn(Optional.empty());
            when(cardMapper.toCard(req)).thenReturn(new Card());
            when(cardRepository.save(any(Card.class))).thenReturn(sysCard);
            when(cardMapper.toCardResponse(sysCard)).thenReturn(cardResponse("cat"));

            CardResponse result = cardService.createSystemCard(req);

            assertThat(result.term()).isEqualTo("cat");
            verify(topicRepository).adjustCardCount(TOPIC_ID, 1);
            verify(deckRepository).adjustCardCount(DECK_ID, 1);
            verify(mediaKeyResolver).publish(emptyImage);
        }

        @Test
        @DisplayName("Create system card failed: topic not found")
        void createSystemCard_Fail_TopicNotFound() {
            CreateSystemCardRequest req = createSystemCardRequest(null, null);
            when(topicRepository.findByIdForUpdate(TOPIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.createSystemCard(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.TOPIC_NOT_FOUND));

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create system card failed: deck is not system owned")
        void createSystemCard_Fail_DeckNotSystem() {
            Deck userDeck = Deck.builder().id(DECK_ID).ownerType(OwnerType.USER).build();
            Topic userTopic = Topic.builder().id(TOPIC_ID).deck(userDeck).build();
            CreateSystemCardRequest req = createSystemCardRequest(null, null);

            when(topicRepository.findByIdForUpdate(TOPIC_ID)).thenReturn(Optional.of(userTopic));

            assertThatThrownBy(() -> cardService.createSystemCard(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_SYSTEM));

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create system card failed: duplicate audio URL in phonetics list")
        void createSystemCard_Fail_DuplicateAudioUrl() {
            Deck sysDeck = systemDeck();
            Topic sysTopic = Topic.builder().id(TOPIC_ID).deck(sysDeck).build();
            CardPhoneticRequest phonetic1 = new CardPhoneticRequest("/kæt/", "verified/audio.mp3", "en-US");
            CardPhoneticRequest phonetic2 = new CardPhoneticRequest("/kæt/", "verified/audio.mp3", "en-UK");
            CreateSystemCardRequest req = createSystemCardRequest(null, List.of(phonetic1, phonetic2));

            MediaKeyResolver.Resolved resolvedAudio = new MediaKeyResolver.Resolved("system/phonetics/audio.mp3", "verified/audio.mp3", List.of());

            when(topicRepository.findByIdForUpdate(TOPIC_ID)).thenReturn(Optional.of(sysTopic));
            when(mediaKeyResolver.forCreate(null, SystemFileModule.CARDS)).thenReturn(new MediaKeyResolver.Resolved(null, null, List.of()));
            when(cardMapper.toCard(req)).thenReturn(new Card());
            when(mediaKeyResolver.forCreate("verified/audio.mp3", SystemFileModule.PHONETICS)).thenReturn(resolvedAudio);
            when(cardPhoneticMapper.toCardPhonetic(phonetic1)).thenReturn(new CardPhonetic());

            assertThatThrownBy(() -> cardService.createSystemCard(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create system card failed: uploads audio key cannot be used for new card")
        void createSystemCard_Fail_UploadsAudioNotAllowedForNewCard() {
            Deck sysDeck = systemDeck();
            Topic sysTopic = Topic.builder().id(TOPIC_ID).deck(sysDeck).build();
            CardPhoneticRequest phonetic = new CardPhoneticRequest("/kæt/", "uploads/unowned.mp3", "en-US");
            CreateSystemCardRequest req = createSystemCardRequest(null, List.of(phonetic));

            when(topicRepository.findByIdForUpdate(TOPIC_ID)).thenReturn(Optional.of(sysTopic));
            when(mediaKeyResolver.forCreate(null, SystemFileModule.CARDS)).thenReturn(new MediaKeyResolver.Resolved(null, null, List.of()));
            when(cardMapper.toCard(req)).thenReturn(new Card());

            assertThatThrownBy(() -> cardService.createSystemCard(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));

            verify(cardRepository, never()).save(any());
        }
    }

    // ─────────────────────── updateSystemCard ───────────────────────

    @Nested
    @DisplayName("updateSystemCard")
    class UpdateSystemCardTests {

        @Test
        @DisplayName("Update system card successfully: replaces media files and deletes obsolete audios")
        void updateSystemCard_Success_WithMediaChanges() {
            Deck sysDeck = systemDeck();
            Topic sysTopic = Topic.builder().id(TOPIC_ID).deck(sysDeck).build();
            Card existingCard = Card.builder()
                    .id(CARD_ID)
                    .deck(sysDeck)
                    .topic(sysTopic)
                    .term("cat")
                    .imageUrl("uploads/old_img.png")
                    .phonetics(new ArrayList<>(List.of(
                            CardPhonetic.builder().audioUrl("uploads/old_audio.mp3").build()
                    )))
                    .build();

            CardPhoneticRequest phoneticReq = new CardPhoneticRequest("/dɒɡ/", "verified/new_audio.mp3", "en-US");
            UpdateSystemCardRequest req = updateSystemCardRequest("verified/new_img.png", List.of(phoneticReq));

            MediaKeyResolver.Resolved resolvedImage = new MediaKeyResolver.Resolved("system/cards/new_img.png", "verified/new_img.png", List.of("uploads/old_img.png"));
            MediaKeyResolver.Resolved resolvedAudio = new MediaKeyResolver.Resolved("system/phonetics/new_audio.mp3", "verified/new_audio.mp3", List.of());

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(existingCard));
            when(mediaKeyResolver.forUpdate("verified/new_img.png", "uploads/old_img.png", SystemFileModule.CARDS)).thenReturn(resolvedImage);
            when(mediaKeyResolver.forCreate("verified/new_audio.mp3", SystemFileModule.PHONETICS)).thenReturn(resolvedAudio);
            when(cardPhoneticMapper.toCardPhonetic(phoneticReq)).thenReturn(new CardPhonetic());
            when(cardMapper.toCardResponse(existingCard)).thenReturn(cardResponse("dog"));

            CardResponse result = cardService.updateSystemCard(CARD_ID, req);

            assertThat(result.term()).isEqualTo("dog");
            verify(cardMapper).updateCardAdmin(existingCard, req);
            assertThat(existingCard.getImageUrl()).isEqualTo("system/cards/new_img.png");
            verify(mediaKeyResolver).publish(resolvedImage);
            verify(mediaKeyResolver).publish(resolvedAudio);

            ArgumentCaptor<FileDeletedEvent> eventCaptor = ArgumentCaptor.forClass(FileDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().fileKeys()).containsExactly("uploads/old_audio.mp3");
        }

        @Test
        @DisplayName("Update system card successfully: replaces system audio and deletes obsolete system audio")
        void updateSystemCard_Success_WithSystemAudioReplacement() {
            Deck sysDeck = systemDeck();
            Topic sysTopic = Topic.builder().id(TOPIC_ID).deck(sysDeck).build();
            String oldSystemAudio = "system/phonetics/old_pronounce.mp3";
            Card existingCard = Card.builder()
                    .id(CARD_ID)
                    .deck(sysDeck)
                    .topic(sysTopic)
                    .term("cat")
                    .imageUrl("system/cards/cat.png")
                    .phonetics(new ArrayList<>(List.of(
                            CardPhonetic.builder().audioUrl(oldSystemAudio).build()
                    )))
                    .build();

            CardPhoneticRequest phoneticReq = new CardPhoneticRequest("/kæt/", "verified/new_pronounce.mp3", "en-US");
            UpdateSystemCardRequest req = updateSystemCardRequest("system/cards/cat.png", List.of(phoneticReq));

            MediaKeyResolver.Resolved resolvedImage = new MediaKeyResolver.Resolved("system/cards/cat.png", null, List.of());
            MediaKeyResolver.Resolved resolvedAudio = new MediaKeyResolver.Resolved("system/phonetics/new_pronounce.mp3", "verified/new_pronounce.mp3", List.of());

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(existingCard));
            when(mediaKeyResolver.forUpdate("system/cards/cat.png", "system/cards/cat.png", SystemFileModule.CARDS)).thenReturn(resolvedImage);
            when(mediaKeyResolver.forCreate("verified/new_pronounce.mp3", SystemFileModule.PHONETICS)).thenReturn(resolvedAudio);
            when(cardPhoneticMapper.toCardPhonetic(phoneticReq)).thenReturn(new CardPhonetic());
            when(cardMapper.toCardResponse(existingCard)).thenReturn(cardResponse("cat"));

            cardService.updateSystemCard(CARD_ID, req);

            verify(mediaKeyResolver).publish(resolvedImage);
            verify(mediaKeyResolver).publish(resolvedAudio);

            ArgumentCaptor<FileDeletedEvent> eventCaptor = ArgumentCaptor.forClass(FileDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().fileKeys()).containsExactly(oldSystemAudio);
        }

        @Test
        @DisplayName("Update system card successfully: retains existing audio without deleting it")
        void updateSystemCard_Success_RetainExistingAudio() {
            Deck sysDeck = systemDeck();
            Topic sysTopic = Topic.builder().id(TOPIC_ID).deck(sysDeck).build();
            Card existingCard = Card.builder()
                    .id(CARD_ID)
                    .deck(sysDeck)
                    .topic(sysTopic)
                    .imageUrl("uploads/img.png")
                    .phonetics(new ArrayList<>(List.of(
                            CardPhonetic.builder().audioUrl("uploads/audio.mp3").build()
                    )))
                    .build();

            CardPhoneticRequest phoneticReq = new CardPhoneticRequest("/kæt/", "uploads/audio.mp3", "en-US");
            UpdateSystemCardRequest req = updateSystemCardRequest("uploads/img.png", List.of(phoneticReq));

            MediaKeyResolver.Resolved resolvedImage = new MediaKeyResolver.Resolved("system/cards/img.png", null, List.of());

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(existingCard));
            when(mediaKeyResolver.forUpdate("uploads/img.png", "uploads/img.png", SystemFileModule.CARDS)).thenReturn(resolvedImage);
            when(cardPhoneticMapper.toCardPhonetic(phoneticReq)).thenReturn(new CardPhonetic());
            when(cardMapper.toCardResponse(existingCard)).thenReturn(cardResponse("cat"));

            cardService.updateSystemCard(CARD_ID, req);

            verify(mediaKeyResolver).publish(resolvedImage);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Update system card successfully: retains existing deep system audio path")
        void updateSystemCard_Success_RetainDeepSystemAudio() {
            Deck sysDeck = systemDeck();
            Topic sysTopic = Topic.builder().id(TOPIC_ID).deck(sysDeck).build();
            String deepAudio = "system/decks/common-1000/animals/audio/cat.mp3";
            String deepImage = "system/decks/common-1000/animals/image/cat.png";
            Card existingCard = Card.builder()
                    .id(CARD_ID)
                    .deck(sysDeck)
                    .topic(sysTopic)
                    .imageUrl(deepImage)
                    .phonetics(new ArrayList<>(List.of(
                            CardPhonetic.builder().audioUrl(deepAudio).build()
                    )))
                    .build();

            CardPhoneticRequest phoneticReq = new CardPhoneticRequest("/kæt/", deepAudio, "en-US");
            UpdateSystemCardRequest req = updateSystemCardRequest(deepImage, List.of(phoneticReq));

            MediaKeyResolver.Resolved resolvedImage = new MediaKeyResolver.Resolved(deepImage, null, List.of());

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(existingCard));
            when(mediaKeyResolver.forUpdate(deepImage, deepImage, SystemFileModule.CARDS)).thenReturn(resolvedImage);
            when(cardPhoneticMapper.toCardPhonetic(phoneticReq)).thenReturn(new CardPhonetic());
            when(cardMapper.toCardResponse(existingCard)).thenReturn(cardResponse("cat"));

            cardService.updateSystemCard(CARD_ID, req);

            verify(mediaKeyResolver).publish(resolvedImage);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Update system card failed: card not found")
        void updateSystemCard_Fail_CardNotFound() {
            UpdateSystemCardRequest req = updateSystemCardRequest(null, null);
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.updateSystemCard(CARD_ID, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CARD_NOT_FOUND));

            verify(cardMapper, never()).updateCardAdmin(any(), any());
        }

        @Test
        @DisplayName("Update system card failed: orphaned card has no deck")
        void updateSystemCard_Fail_OrphanedCard() {
            Card orphanedCard = Card.builder().id(CARD_ID).deck(null).build();
            UpdateSystemCardRequest req = updateSystemCardRequest(null, null);

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(orphanedCard));

            assertThatThrownBy(() -> cardService.updateSystemCard(CARD_ID, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CARD_NOT_AVAILABLE));

            verify(cardMapper, never()).updateCardAdmin(any(), any());
        }

        @Test
        @DisplayName("Update system card failed: deck is not system owned")
        void updateSystemCard_Fail_DeckNotSystem() {
            Deck userDeck = Deck.builder().id(DECK_ID).ownerType(OwnerType.USER).build();
            Card userCard = Card.builder().id(CARD_ID).deck(userDeck).build();
            UpdateSystemCardRequest req = updateSystemCardRequest(null, null);

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(userCard));

            assertThatThrownBy(() -> cardService.updateSystemCard(CARD_ID, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_SYSTEM));

            verify(cardMapper, never()).updateCardAdmin(any(), any());
        }

        @Test
        @DisplayName("Update system card failed: duplicate audio URL in phonetics list")
        void updateSystemCard_Fail_DuplicateAudioUrl() {
            Deck sysDeck = systemDeck();
            Card existingCard = Card.builder().id(CARD_ID).deck(sysDeck).phonetics(new ArrayList<>()).build();
            CardPhoneticRequest phonetic1 = new CardPhoneticRequest("/kæt/", "verified/audio.mp3", "en-US");
            CardPhoneticRequest phonetic2 = new CardPhoneticRequest("/kæt/", "verified/audio.mp3", "en-UK");
            UpdateSystemCardRequest req = updateSystemCardRequest(null, List.of(phonetic1, phonetic2));

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(existingCard));
            when(mediaKeyResolver.forUpdate(any(), any(), eq(SystemFileModule.CARDS))).thenReturn(new MediaKeyResolver.Resolved(null, null, List.of()));
            when(mediaKeyResolver.forCreate("verified/audio.mp3", SystemFileModule.PHONETICS))
                    .thenReturn(new MediaKeyResolver.Resolved("system/phonetics/audio.mp3", "verified/audio.mp3", List.of()));
            when(cardPhoneticMapper.toCardPhonetic(phonetic1)).thenReturn(new CardPhonetic());

            assertThatThrownBy(() -> cardService.updateSystemCard(CARD_ID, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));
        }

        @Test
        @DisplayName("Update system card failed: unowned uploads audio key requested")
        void updateSystemCard_Fail_UnownedUploadsAudio() {
            Deck sysDeck = systemDeck();
            Card existingCard = Card.builder().id(CARD_ID).deck(sysDeck).phonetics(new ArrayList<>()).build();
            CardPhoneticRequest phonetic = new CardPhoneticRequest("/kæt/", "uploads/not_mine.mp3", "en-US");
            UpdateSystemCardRequest req = updateSystemCardRequest(null, List.of(phonetic));

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(existingCard));
            when(mediaKeyResolver.forUpdate(any(), any(), eq(SystemFileModule.CARDS))).thenReturn(new MediaKeyResolver.Resolved(null, null, List.of()));

            assertThatThrownBy(() -> cardService.updateSystemCard(CARD_ID, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));
        }
    }

    // ─────────────────────── reorderSystemCards ───────────────────────

    @Nested
    @DisplayName("reorderSystemCards")
    class ReorderSystemCardsTests {

        @Test
        @DisplayName("Reorder system cards successfully")
        void reorderSystemCards_Success() {
            Topic sysTopic = systemTopic();
            Card card1 = Card.builder().id(101L).topic(sysTopic).order(1).build();
            Card card2 = Card.builder().id(102L).topic(sysTopic).order(2).build();

            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(sysTopic));
            when(cardRepository.countByTopicId(TOPIC_ID)).thenReturn(2L);
            when(cardRepository.findAllById(List.of(102L, 101L))).thenReturn(List.of(card1, card2));

            cardService.reorderSystemCards(TOPIC_ID, List.of(102L, 101L));

            assertThat(card2.getOrder()).isEqualTo(1);
            assertThat(card1.getOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("Reorder system cards failed: topic not found")
        void reorderSystemCards_Fail_TopicNotFound() {
            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.reorderSystemCards(TOPIC_ID, List.of(101L)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.TOPIC_NOT_FOUND));

            verify(cardRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("Reorder system cards failed: deck is not system owned")
        void reorderSystemCards_Fail_DeckNotSystem() {
            Topic userTopic = Topic.builder().id(TOPIC_ID).deck(Deck.builder().ownerType(OwnerType.USER).build()).build();
            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(userTopic));

            assertThatThrownBy(() -> cardService.reorderSystemCards(TOPIC_ID, List.of(101L)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_SYSTEM));

            verify(cardRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("Reorder system cards failed: duplicate IDs in ordered list")
        void reorderSystemCards_Fail_DuplicateIds() {
            Topic sysTopic = systemTopic();
            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(sysTopic));

            assertThatThrownBy(() -> cardService.reorderSystemCards(TOPIC_ID, List.of(101L, 101L)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));

            verify(cardRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("Reorder system cards failed: size mismatch with count in topic")
        void reorderSystemCards_Fail_SizeMismatch() {
            Topic sysTopic = systemTopic();
            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(sysTopic));
            when(cardRepository.countByTopicId(TOPIC_ID)).thenReturn(3L);

            assertThatThrownBy(() -> cardService.reorderSystemCards(TOPIC_ID, List.of(101L, 102L)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));

            verify(cardRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("Reorder system cards failed: card not found in topic")
        void reorderSystemCards_Fail_CardNotFound() {
            Topic sysTopic = systemTopic();
            Card card1 = Card.builder().id(101L).topic(sysTopic).order(1).build();

            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(sysTopic));
            when(cardRepository.countByTopicId(TOPIC_ID)).thenReturn(2L);
            when(cardRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(card1));

            assertThatThrownBy(() -> cardService.reorderSystemCards(TOPIC_ID, List.of(101L, 102L)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CARD_NOT_FOUND));
        }

        @Test
        @DisplayName("Reorder system cards failed: card belongs to a different topic or topic is null")
        void reorderSystemCards_Fail_CardNotInTopic() {
            Topic sysTopic = systemTopic();
            Topic otherTopic = Topic.builder().id(999L).build();
            Card card1 = Card.builder().id(101L).topic(otherTopic).order(1).build();

            when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(sysTopic));
            when(cardRepository.countByTopicId(TOPIC_ID)).thenReturn(1L);
            when(cardRepository.findAllById(List.of(101L))).thenReturn(List.of(card1));

            assertThatThrownBy(() -> cardService.reorderSystemCards(TOPIC_ID, List.of(101L)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));
        }
    }

    // ─────────────────────── deleteSystemCard ───────────────────────

    @Nested
    @DisplayName("deleteSystemCard")
    class DeleteSystemCardTests {

        @Test
        @DisplayName("Delete system card successfully with media files: decrements counters and publishes FileDeletedEvent")
        void deleteSystemCard_Success_WithMediaFiles() {
            Deck sysDeck = systemDeck();
            Topic sysTopic = systemTopic();
            Card sysCard = Card.builder()
                    .id(CARD_ID)
                    .deck(sysDeck)
                    .topic(sysTopic)
                    .imageUrl("uploads/cat.png")
                    .phonetics(List.of(CardPhonetic.builder().audioUrl("uploads/cat.mp3").build()))
                    .build();

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(sysCard));

            cardService.deleteSystemCard(CARD_ID);

            verify(cardRepository).delete(sysCard);
            verify(topicRepository).adjustCardCount(TOPIC_ID, -1);
            verify(deckRepository).adjustCardCount(DECK_ID, -1);

            ArgumentCaptor<FileDeletedEvent> captor = ArgumentCaptor.forClass(FileDeletedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().fileKeys()).containsExactlyInAnyOrder("uploads/cat.png", "uploads/cat.mp3");
        }

        @Test
        @DisplayName("Delete system card successfully without media files: decrements counters and does not publish event")
        void deleteSystemCard_Success_WithoutMediaFiles() {
            Deck sysDeck = systemDeck();
            Topic sysTopic = systemTopic();
            Card sysCard = Card.builder()
                    .id(CARD_ID)
                    .deck(sysDeck)
                    .topic(sysTopic)
                    .build();

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(sysCard));

            cardService.deleteSystemCard(CARD_ID);

            verify(cardRepository).delete(sysCard);
            verify(topicRepository).adjustCardCount(TOPIC_ID, -1);
            verify(deckRepository).adjustCardCount(DECK_ID, -1);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Delete system card failed: card not found")
        void deleteSystemCard_Fail_CardNotFound() {
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.deleteSystemCard(CARD_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CARD_NOT_FOUND));

            verify(cardRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Delete system card failed: card is orphaned (deck is null)")
        void deleteSystemCard_Fail_OrphanedCard() {
            Card orphan = Card.builder().id(CARD_ID).deck(null).build();
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(orphan));

            assertThatThrownBy(() -> cardService.deleteSystemCard(CARD_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CARD_NOT_AVAILABLE));

            verify(cardRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Delete system card failed: deck is not system owned")
        void deleteSystemCard_Fail_DeckNotSystem() {
            Deck userDeck = Deck.builder().id(DECK_ID).ownerType(OwnerType.USER).build();
            Card userCard = Card.builder().id(CARD_ID).deck(userDeck).build();
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(userCard));

            assertThatThrownBy(() -> cardService.deleteSystemCard(CARD_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_SYSTEM));

            verify(cardRepository, never()).delete(any());
        }
    }

    // ─────────────────────── getOrphanedCardStats ───────────────────────

    @Nested
    @DisplayName("getOrphanedCardStats")
    class GetOrphanedCardStatsTests {

        @Test
        @DisplayName("Get orphaned card stats successfully")
        void getOrphanedCardStats_Success() {
            when(cardRepository.countOrphanedCards()).thenReturn(10L);
            when(cardRepository.countSafeToDeleteOrphans()).thenReturn(6L);

            OrphanedCardStatsResponse result = cardService.getOrphanedCardStats();

            assertThat(result.totalOrphanedCards()).isEqualTo(10L);
            assertThat(result.safeToDeleteCards()).isEqualTo(6L);
            assertThat(result.retainedInStudyCards()).isEqualTo(4L);
            verify(cardRepository).countOrphanedCards();
            verify(cardRepository).countSafeToDeleteOrphans();
        }
    }

    // ─────────────────────── getOrphanedCards ───────────────────────

    @Nested
    @DisplayName("getOrphanedCards")
    class GetOrphanedCardsTests {

        @Test
        @DisplayName("Get orphaned cards page successfully")
        void getOrphanedCards_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Card card1 = Card.builder().id(101L).build();
            CardResponse response1 = cardResponse("cat");
            Page<Card> page = new PageImpl<>(List.of(card1), pageable, 1);

            when(cardRepository.findOrphanedCards(pageable)).thenReturn(page);
            when(cardMapper.toCardResponse(card1)).thenReturn(response1);

            PageResponse<CardResponse> result = cardService.getOrphanedCards(pageable);

            assertThat(result.content()).containsExactly(response1);
            assertThat(result.totalElements()).isEqualTo(1L);
            verify(cardRepository).findOrphanedCards(pageable);
        }
    }

    // ─────────────────────── cleanupOrphanedCards ───────────────────────

    @Nested
    @DisplayName("cleanupOrphanedCards")
    class CleanupOrphanedCardsTests {

        @Test
        @DisplayName("Cleanup orphaned cards successfully when no safe orphans are found")
        void cleanupOrphanedCards_Success_NoOrphansFound() {
            when(cardRepository.findSafeToDeleteOrphanIds(500)).thenReturn(List.of());

            OrphanedCardCleanupResponse result = cardService.cleanupOrphanedCards();

            assertThat(result.scannedCount()).isZero();
            assertThat(result.deletedCardCount()).isZero();
            assertThat(result.deletedFileCount()).isZero();
            verify(cardRepository, never()).deleteAll(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Cleanup orphaned cards successfully with media files deleted")
        void cleanupOrphanedCards_Success_WithMediaFiles() {
            List<Long> safeIds = List.of(101L, 102L);
            Card card1 = Card.builder().id(101L).imageUrl("uploads/img.png").build();
            Card card2 = Card.builder().id(102L).phonetics(List.of(CardPhonetic.builder().audioUrl("uploads/audio.mp3").build())).build();
            List<Card> safeCards = List.of(card1, card2);

            when(cardRepository.findSafeToDeleteOrphanIds(500)).thenReturn(safeIds);
            when(cardRepository.findAllByIdWithPhonetics(safeIds)).thenReturn(safeCards);

            OrphanedCardCleanupResponse result = cardService.cleanupOrphanedCards();

            assertThat(result.scannedCount()).isEqualTo(2);
            assertThat(result.deletedCardCount()).isEqualTo(2);
            assertThat(result.deletedFileCount()).isEqualTo(2);
            verify(cardRepository).deleteAll(safeCards);

            ArgumentCaptor<FileDeletedEvent> captor = ArgumentCaptor.forClass(FileDeletedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().fileKeys()).containsExactlyInAnyOrder("uploads/img.png", "uploads/audio.mp3");
        }

        @Test
        @DisplayName("Cleanup orphaned cards successfully without media files")
        void cleanupOrphanedCards_Success_WithoutMediaFiles() {
            List<Long> safeIds = List.of(101L);
            Card card1 = Card.builder().id(101L).build();
            List<Card> safeCards = List.of(card1);

            when(cardRepository.findSafeToDeleteOrphanIds(500)).thenReturn(safeIds);
            when(cardRepository.findAllByIdWithPhonetics(safeIds)).thenReturn(safeCards);

            OrphanedCardCleanupResponse result = cardService.cleanupOrphanedCards();

            assertThat(result.scannedCount()).isEqualTo(1);
            assertThat(result.deletedCardCount()).isEqualTo(1);
            assertThat(result.deletedFileCount()).isZero();
            verify(cardRepository).deleteAll(safeCards);
            verifyNoInteractions(eventPublisher);
        }
    }
}
