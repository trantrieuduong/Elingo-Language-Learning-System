package com.elingo.learning.flashcard.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.response.ReviewCardResponse;
import com.elingo.learning.flashcard.entity.UserCardState;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.learning.flashcard.service.impl.FlashcardServiceImpl;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class FlashcardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserCardStateRepository userCardStateRepository;

    @Mock
    private SpacedRepetitionService spacedRepetitionService;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private FlashcardServiceImpl flashcardService;//

    private User testUser;
    private Card testCard;
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

        Deck testDeck = Deck.builder()
                .id(DECK_ID)
                .build();

        Topic testTopic = Topic.builder()
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
                .deck(testDeck)
                .topic(testTopic)
                .flagsStarred(false)
                .flagsHidden(false)
                .srsEaseFactor(new BigDecimal("2.50"))
                .srsInterval(0)
                .build();
    }

    @Nested
    @DisplayName("submitSrsReview")
    class SubmitSrsReviewTests {
        @Test
        @DisplayName("Submit SRS review successfully: State exists")
        void submitSrsReview_Success_StateExists() {
            SrsReviewRequest request = new SrsReviewRequest(3);
            when(userCardStateRepository.findByCardIdAndUserId(CARD_ID, USER_ID)).thenReturn(Optional.of(testState));

            flashcardService.submitSrsReview(USER_ID, CARD_ID, request);

            verify(spacedRepetitionService).calculateNextSRS(testState, 3);
            verify(userCardStateRepository).save(testState);
        }

        @Test
        @DisplayName("Submit SRS review successfully: State not exists")
        void submitSrsReview_Success_StateNotExists() {
            SrsReviewRequest request = new SrsReviewRequest(3);
            when(userCardStateRepository.findByCardIdAndUserId(CARD_ID, USER_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            flashcardService.submitSrsReview(USER_ID, CARD_ID, request);

            verify(spacedRepetitionService).calculateNextSRS(any(UserCardState.class), eq(3));
            verify(userCardStateRepository).save(any(UserCardState.class));
        }

        @Test
        @DisplayName("Submit SRS review failed: Card not found")
        void submitSrsReview_Fail_CardNotFound() {
            SrsReviewRequest request = new SrsReviewRequest(3);
            when(userCardStateRepository.findByCardIdAndUserId(CARD_ID, USER_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> flashcardService.submitSrsReview(USER_ID, CARD_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CARD_NOT_FOUND));

            verify(spacedRepetitionService, never()).calculateNextSRS(any(UserCardState.class), anyInt());
            verify(userCardStateRepository, never()).save(any(UserCardState.class));
        }

        @Test
        @DisplayName("Submit SRS review failed: User not found")
        void submitSrsReview_Fail_UserNotFound() {
            SrsReviewRequest request = new SrsReviewRequest(3);
            when(userCardStateRepository.findByCardIdAndUserId(CARD_ID, USER_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> flashcardService.submitSrsReview(USER_ID, CARD_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verify(spacedRepetitionService, never()).calculateNextSRS(any(UserCardState.class), anyInt());
            verify(userCardStateRepository, never()).save(any(UserCardState.class));
        }
    }

    @Nested
    @DisplayName("toggleStar")
    class ToggleStarTests {
        @Test
        @DisplayName("Toggle star successfully: State exists")
        void toggleStar_Success_StateExists() {
            testState.setFlagsStarred(false);
            when(userCardStateRepository.findByCardIdAndUserId(CARD_ID, USER_ID)).thenReturn(Optional.of(testState));

            flashcardService.toggleStar(USER_ID, CARD_ID);

            assertThat(testState.getFlagsStarred()).isTrue();
            verify(userCardStateRepository).save(testState);
        }

        @Test
        @DisplayName("Toggle star successfully: State not exists")
        void toggleStar_Success_StateNotExists() {
            when(userCardStateRepository.findByCardIdAndUserId(CARD_ID, USER_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            flashcardService.toggleStar(USER_ID, CARD_ID);

            verify(userCardStateRepository).save(argThat(state ->
                    state.getFlagsStarred() != null && state.getFlagsStarred()
            ));
        }
    }

    @Nested
    @DisplayName("toggleHide")
    class ToggleHideTests {
        @Test
        @DisplayName("Toggle hide successfully: State exists")
        void toggleHide_Success_StateExists() {
            testState.setFlagsHidden(false);
            when(userCardStateRepository.findByCardIdAndUserId(CARD_ID, USER_ID)).thenReturn(Optional.of(testState));

            flashcardService.toggleHide(USER_ID, CARD_ID);

            assertThat(testState.getFlagsHidden()).isTrue();
            verify(userCardStateRepository).save(testState);
        }

        @Test
        @DisplayName("Toggle hide successfully: State not exists")
        void toggleHide_Success_StateNotExists() {
            when(userCardStateRepository.findByCardIdAndUserId(CARD_ID, USER_ID)).thenReturn(Optional.empty());
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            flashcardService.toggleHide(USER_ID, CARD_ID);

            verify(userCardStateRepository).save(argThat(state ->
                    state.getFlagsHidden() != null && state.getFlagsHidden()
            ));
        }
    }

    @Nested
    @DisplayName("getCardsForReview")
    class GetCardsForReviewTests {
        @Test
        @DisplayName("Get cards for review successfully")
        void getCardsForReview_Success() {
            testState.setSrsNextReviewAt(LocalDateTime.now().plusDays(1));
            when(userCardStateRepository.findCardsForReview(USER_ID)).thenReturn(List.of(testState));

            CardResponse mockCardResponse = mock(CardResponse.class);
            when(cardMapper.toCardResponse(testCard)).thenReturn(mockCardResponse);

            List<ReviewCardResponse> result = flashcardService.getCardsForReview(USER_ID);

            assertThat(result).hasSize(1);
            ReviewCardResponse reviewResponse = result.getFirst();
            assertThat(reviewResponse.card()).isEqualTo(mockCardResponse);
            assertThat(reviewResponse.srsNextReviewAt()).isEqualTo(testState.getSrsNextReviewAt());
            assertThat(reviewResponse.flagsStarred()).isEqualTo(testState.getFlagsStarred());
            assertThat(reviewResponse.flagsHidden()).isEqualTo(testState.getFlagsHidden());
        }
    }
}