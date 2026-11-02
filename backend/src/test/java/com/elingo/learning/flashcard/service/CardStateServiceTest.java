package com.elingo.learning.flashcard.service;

import com.elingo.common.event.XpGainedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.service.RedisService;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.response.CardLearningState;
import com.elingo.learning.flashcard.entity.UserCardState;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.learning.flashcard.service.impl.CardStateServiceImpl;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.service.DeckAccessPolicy;
import com.elingo.vocabulary.support.TestDeckAccessView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CardStateServiceTest {

    @Mock
    private UserCardStateRepository stateRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private DeckAccessPolicy accessPolicy;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private SpacedRepetitionService spacedRepetitionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CardStateServiceImpl cardStateService;

    private User testUser;
    private Card testCard;
    private Topic testTopic;
    private Deck testDeck;
    private UserCardState testState;

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 100L;
    private static final Long DECK_ID = 200L;
    private static final Long TOPIC_ID = 300L;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .username("testuser")
                .build();

        testDeck = Deck.builder()
                .id(DECK_ID)
                .status(DeckStatus.PUBLISHED)
                .isPremium(false)
                .build();

        testTopic = Topic.builder()
                .id(TOPIC_ID)
                .build();

        testCard = Card.builder()
                .id(CARD_ID)
                .deck(testDeck)
                .topic(testTopic)
                .build();

        testState = UserCardState.builder()
                .id(1L)
                .user(testUser)
                .card(testCard)
                .flagsStarred(false)
                .flagsHidden(false)
                .srsEaseFactor(new BigDecimal("2.50"))
                .srsInterval(0)
                .build();
    }

    private DeckRepository.DeckAccessView deckAccessView(DeckStatus status, boolean isPremium) {
        return TestDeckAccessView.of(DECK_ID, status, OwnerType.SYSTEM, null, isPremium);
    }

    @Nested
    @DisplayName("submitReview")
    class SubmitReviewTests {
        @Test
        @DisplayName("Submit review successfully: state already exists")
        void submitReview_Success_StateExists() {
            SrsReviewRequest request = new SrsReviewRequest(3);
            when(redisService.incrementWithTtl(any(), any())).thenReturn(1L);
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.of(testState));

            cardStateService.submitReview(USER_ID, CARD_ID, request);

            verify(spacedRepetitionService).calculateNextSRS(testState, 3);
            verify(eventPublisher).publishEvent(any(XpGainedEvent.class));
            // Existing entity is updated through JPA dirty checking, no explicit save needed
            verify(stateRepository, never()).save(any(UserCardState.class));
        }

        @Test
        @DisplayName("Submit review successfully: state does not exist yet")
        void submitReview_Success_StateNotExists() {
            SrsReviewRequest request = new SrsReviewRequest(3);
            when(redisService.incrementWithTtl(any(), any())).thenReturn(1L);
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(deckAccessView(DeckStatus.PUBLISHED, false)));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(stateRepository.save(any(UserCardState.class)))
                    .thenAnswer(AdditionalAnswers.returnsFirstArg());

            cardStateService.submitReview(USER_ID, CARD_ID, request);

            verify(spacedRepetitionService).calculateNextSRS(any(UserCardState.class), eq(3));
            verify(stateRepository).save(any(UserCardState.class));
            verify(eventPublisher).publishEvent(any(XpGainedEvent.class));
        }

        @Test
        @DisplayName("Submit review failed: duplicate submission is rejected by the rate guard")
        void submitReview_Fail_DuplicateSubmission() {
            SrsReviewRequest request = new SrsReviewRequest(3);
            when(redisService.incrementWithTtl(any(), any())).thenReturn(2L);

            assertThatThrownBy(() -> cardStateService.submitReview(USER_ID, CARD_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.RATE_LIMIT_EXCEEDED));

            verify(spacedRepetitionService, never()).calculateNextSRS(any(UserCardState.class), anyInt());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Submit review failed: card not found")
        void submitReview_Fail_CardNotFound() {
            SrsReviewRequest request = new SrsReviewRequest(3);
            when(redisService.incrementWithTtl(any(), any())).thenReturn(1L);
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardStateService.submitReview(USER_ID, CARD_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CARD_NOT_FOUND));

            verify(spacedRepetitionService, never()).calculateNextSRS(any(UserCardState.class), anyInt());
            verify(stateRepository, never()).save(any(UserCardState.class));
        }

        @Test
        @DisplayName("Submit review failed: card is not attached to any deck")
        void submitReview_Fail_CardNotAvailable() {
            SrsReviewRequest request = new SrsReviewRequest(3);
            Card detachedCard = Card.builder().id(CARD_ID).build();
            when(redisService.incrementWithTtl(any(), any())).thenReturn(1L);
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(detachedCard));

            assertThatThrownBy(() -> cardStateService.submitReview(USER_ID, CARD_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CARD_NOT_AVAILABLE));

            verify(spacedRepetitionService, never()).calculateNextSRS(any(UserCardState.class), anyInt());
            verify(stateRepository, never()).save(any(UserCardState.class));
        }

        @Test
        @DisplayName("Submit review failed: user not found")
        void submitReview_Fail_UserNotFound() {
            SrsReviewRequest request = new SrsReviewRequest(3);
            when(redisService.incrementWithTtl(any(), any())).thenReturn(1L);
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(deckAccessView(DeckStatus.PUBLISHED, false)));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardStateService.submitReview(USER_ID, CARD_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verify(spacedRepetitionService, never()).calculateNextSRS(any(UserCardState.class), anyInt());
            verify(stateRepository, never()).save(any(UserCardState.class));
        }

        @Test
        @DisplayName("Submit review failed: deck access policy rejects the deck")
        void submitReview_Fail_DeckAccessDenied() {
            SrsReviewRequest request = new SrsReviewRequest(3);
            when(redisService.incrementWithTtl(any(), any())).thenReturn(1L);
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(deckRepository.findAccessViewById(DECK_ID))
                    .thenReturn(Optional.of(deckAccessView(DeckStatus.DRAFT, false)));
            doThrow(new AppException(AppError.DECK_NOT_FOUND))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> cardStateService.submitReview(USER_ID, CARD_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_FOUND));

            verify(spacedRepetitionService, never()).calculateNextSRS(any(UserCardState.class), anyInt());
            verify(stateRepository, never()).save(any(UserCardState.class));
        }

        @Test
        @DisplayName("Submit review failed: premium deck requires an active subscription")
        void submitReview_Fail_PremiumRequired() {
            SrsReviewRequest request = new SrsReviewRequest(3);
            when(redisService.incrementWithTtl(any(), any())).thenReturn(1L);
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(deckRepository.findAccessViewById(DECK_ID))
                    .thenReturn(Optional.of(deckAccessView(DeckStatus.PUBLISHED, true)));
            doThrow(new AppException(AppError.PREMIUM_REQUIRED))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> cardStateService.submitReview(USER_ID, CARD_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.PREMIUM_REQUIRED));

            verify(spacedRepetitionService, never()).calculateNextSRS(any(UserCardState.class), anyInt());
            verify(stateRepository, never()).save(any(UserCardState.class));
        }
    }

    @Nested
    @DisplayName("setStarred")
    class SetStarredTests {
        @Test
        @DisplayName("Set starred successfully: state exists")
        void setStarred_Success_StateExists() {
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.of(testState));

            cardStateService.setStarred(USER_ID, CARD_ID, true);

            assertThat(testState.getFlagsStarred()).isTrue();
            verify(stateRepository, never()).save(any(UserCardState.class));
        }

        @Test
        @DisplayName("Set starred successfully: state does not exist yet")
        void setStarred_Success_StateNotExists() {
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(deckAccessView(DeckStatus.PUBLISHED, false)));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(stateRepository.save(any(UserCardState.class)))
                    .thenAnswer(AdditionalAnswers.returnsFirstArg());

            cardStateService.setStarred(USER_ID, CARD_ID, true);

            verify(stateRepository).save(argThat(state ->
                    Boolean.TRUE.equals(state.getFlagsStarred())
            ));
        }

        @Test
        @DisplayName("Set starred failed: unpublished deck")
        void setStarred_Fail_DeckNotPublished() {
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(deckRepository.findAccessViewById(DECK_ID))
                    .thenReturn(Optional.of(deckAccessView(DeckStatus.DRAFT, false)));
            doThrow(new AppException(AppError.DECK_NOT_FOUND))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> cardStateService.setStarred(USER_ID, CARD_ID, true))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_FOUND));

            verify(stateRepository, never()).save(any(UserCardState.class));
        }

        @Test
        @DisplayName("Set starred failed: premium deck without active subscription")
        void setStarred_Fail_PremiumRequired() {
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(deckRepository.findAccessViewById(DECK_ID))
                    .thenReturn(Optional.of(deckAccessView(DeckStatus.PUBLISHED, true)));
            doThrow(new AppException(AppError.PREMIUM_REQUIRED))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> cardStateService.setStarred(USER_ID, CARD_ID, true))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.PREMIUM_REQUIRED));

            verify(stateRepository, never()).save(any(UserCardState.class));
        }
    }

    @Nested
    @DisplayName("setHidden")
    class SetHiddenTests {
        @Test
        @DisplayName("Set hidden successfully: state exists")
        void setHidden_Success_StateExists() {
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.of(testState));

            cardStateService.setHidden(USER_ID, CARD_ID, true);

            assertThat(testState.getFlagsHidden()).isTrue();
            verify(stateRepository, never()).save(any(UserCardState.class));
        }

        @Test
        @DisplayName("Set hidden successfully: state does not exist yet")
        void setHidden_Success_StateNotExists() {
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(deckRepository.findAccessViewById(DECK_ID)).thenReturn(Optional.of(deckAccessView(DeckStatus.PUBLISHED, false)));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(stateRepository.save(any(UserCardState.class)))
                    .thenAnswer(AdditionalAnswers.returnsFirstArg());

            cardStateService.setHidden(USER_ID, CARD_ID, true);

            verify(stateRepository).save(argThat(state ->
                    Boolean.TRUE.equals(state.getFlagsHidden())
            ));
        }

        @Test
        @DisplayName("Set hidden failed: unpublished deck")
        void setHidden_Fail_DeckNotPublished() {
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(deckRepository.findAccessViewById(DECK_ID))
                    .thenReturn(Optional.of(deckAccessView(DeckStatus.DRAFT, false)));
            doThrow(new AppException(AppError.DECK_NOT_FOUND))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> cardStateService.setHidden(USER_ID, CARD_ID, true))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_FOUND));

            verify(stateRepository, never()).save(any(UserCardState.class));
        }

        @Test
        @DisplayName("Set hidden failed: premium deck without active subscription")
        void setHidden_Fail_PremiumRequired() {
            when(stateRepository.findForUpdate(USER_ID, CARD_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(deckRepository.findAccessViewById(DECK_ID))
                    .thenReturn(Optional.of(deckAccessView(DeckStatus.PUBLISHED, true)));
            doThrow(new AppException(AppError.PREMIUM_REQUIRED))
                    .when(accessPolicy).assertCanLearn(any(), eq(USER_ID));

            assertThatThrownBy(() -> cardStateService.setHidden(USER_ID, CARD_ID, true))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.PREMIUM_REQUIRED));

            verify(stateRepository, never()).save(any(UserCardState.class));
        }
    }

    @Nested
    @DisplayName("getDueCards")
    class GetDueCardsTests {
        @Test
        @DisplayName("Get due cards successfully")
        void getDueCards_Success() {
            testState.setSrsNextReviewAt(LocalDateTime.now().minusHours(1));
            Pageable pageable = PageRequest.of(0, 100);
            when(stateRepository.findDue(eq(USER_ID), any(LocalDateTime.class), eq(pageable)))
                    .thenReturn(List.of(testState));

            CardResponse mockCardResponse = mock(CardResponse.class);
            when(cardMapper.toCardResponse(eq(testCard), any(CardLearningState.class))).thenReturn(mockCardResponse);

            List<CardResponse> result = cardStateService.getDueCards(USER_ID, 100);

            assertThat(result).containsExactly(mockCardResponse);
        }

        @Test
        @DisplayName("Get due cards returns empty list when nothing is due")
        void getDueCards_EmptyList() {
            Pageable pageable = PageRequest.of(0, 100);
            when(stateRepository.findDue(eq(USER_ID), any(LocalDateTime.class), eq(pageable)))
                    .thenReturn(List.of());

            List<CardResponse> result = cardStateService.getDueCards(USER_ID, 100);

            assertThat(result).isEmpty();
            verifyNoInteractions(cardMapper);
        }
    }

    @Nested
    @DisplayName("getStates")
    class GetStatesTests {
        @Test
        @DisplayName("Get states successfully")
        void getStates_Success() {
            UserCardStateRepository.CardStateView view = mock(UserCardStateRepository.CardStateView.class);
            when(view.getCardId()).thenReturn(CARD_ID);
            when(view.getSrsLastGrade()).thenReturn((short) 2);
            when(view.getFlagsStarred()).thenReturn(true);
            when(view.getFlagsHidden()).thenReturn(false);
            when(stateRepository.findStateViews(USER_ID, List.of(CARD_ID))).thenReturn(List.of(view));

            Map<Long, CardLearningState> result = cardStateService.getStates(USER_ID, List.of(CARD_ID));

            assertThat(result).hasSize(1);
            assertThat(result.get(CARD_ID).learned()).isTrue();
            assertThat(result.get(CARD_ID).starred()).isTrue();
            assertThat(result.get(CARD_ID).hidden()).isFalse();
        }

        @Test
        @DisplayName("Get states returns empty map when user is anonymous or no card ids given")
        void getStates_Empty() {
            assertThat(cardStateService.getStates(null, List.of(CARD_ID))).isEmpty();
            assertThat(cardStateService.getStates(USER_ID, List.of())).isEmpty();
            verifyNoInteractions(stateRepository);
        }
    }
}