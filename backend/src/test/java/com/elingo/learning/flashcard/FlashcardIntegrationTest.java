package com.elingo.learning.flashcard;

import com.elingo.BaseIntegrationTest;
import com.elingo.auth.service.JwtService;
import com.elingo.learning.flashcard.entity.UserCardState;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FlashcardIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCardStateRepository userCardStateRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .username("flashcard_tester")
                .email("flashcard_tester@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Flashcard Tester")
                .isActive(true)
                .isVerified(true)
                .build());

        String authorities = testUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        authToken = jwtService.generateAccessToken(testUser.getId().toString(), authorities);
    }

    private Deck persistDeck(String title, String slug) {
        return deckRepository.save(Deck.builder()
                .title(title)
                .slug(slug)
                .status(DeckStatus.PUBLISHED)
                .ownerType(OwnerType.SYSTEM)
                .build());
    }

    private Topic persistTopic(Deck deck, String name, String slug) {
        return topicRepository.save(Topic.builder()
                .deck(deck)
                .name(name)
                .slug(slug)
                .build());
    }

    private Card persistCard(Deck deck, Topic topic, String term, String translation, int order) {
        return cardRepository.save(Card.builder()
                .deck(deck)
                .topic(topic)
                .term(term)
                .translation(translation)
                .order(order)
                .build());
    }

    private void persistUserCardState(User user, Card card) {
        userCardStateRepository.save(UserCardState.builder()
                .user(user)
                .card(card)
                .deck(card.getDeck())
                .topic(card.getTopic())
                .srsNextReviewAt(LocalDateTime.now().plusDays(1))
                .build());
    }

    @Nested
    @DisplayName("getCardsByTopic")
    class GetCardsByTopicTests {
        private Deck deck;
        private Topic topic;

        @BeforeEach
        void setUp() {
            deck = persistDeck("Animals Deck", "animals-deck");
            topic = persistTopic(deck, "Animals", "animals");
        }

        private ResultActions performGetCardsByTopic(long topicId) throws Exception {
            return mockMvc.perform(get("/flashcard/topics/{topicId}/cards", topicId)
                    .header("Authorization", "Bearer " + authToken));
        }

        @Test
        @DisplayName("Get cards by topic: Success")
        void testGetCardsByTopic_Success() throws Exception {
            persistCard(deck, topic, "cat", "con meo", 1);
            persistCard(deck, topic, "dog", "con cho", 2);

            performGetCardsByTopic(topic.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].term").value("cat"))
                    .andExpect(jsonPath("$.data[1].term").value("dog"));
        }

        @Test
        @DisplayName("Get cards by topic: Success - exclude reviewed cards of current user")
        void testGetCardsByTopic_ExcludesReviewedCardOfCurrentUser() throws Exception {
            Card reviewedCard = persistCard(deck, topic, "cat", "con meo", 1);
            persistCard(deck, topic, "dog", "con cho", 2);
            persistUserCardState(testUser, reviewedCard);

            performGetCardsByTopic(topic.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].term").value("dog"));
        }

        @Test
        @DisplayName("Get cards by topic: Success - return empty when all cards reviewed")
        void testGetCardsByTopic_ReturnsEmpty_WhenAllCardsReviewed() throws Exception {
            Card card1 = persistCard(deck, topic, "cat", "con meo", 1);
            Card card2 = persistCard(deck, topic, "dog", "con cho", 2);
            persistUserCardState(testUser, card1);
            persistUserCardState(testUser, card2);

            performGetCardsByTopic(topic.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("Get cards by topic: Success - return empty when topic has no cards")
        void testGetCardsByTopic_ReturnsEmpty_WhenTopicHasNoCards() throws Exception {
            performGetCardsByTopic(topic.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("Get cards by topic: Success - other user state does not filter")
        void testGetCardsByTopic_OtherUserStateDoesNotFilter() throws Exception {
            Card card = persistCard(deck, topic, "cat", "con meo", 1);

            User otherUser = userRepository.save(User.builder()
                    .username("other_user_fc")
                    .email("other_fc@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other User")
                    .isActive(true)
                    .isVerified(true)
                    .build());
            persistUserCardState(otherUser, card);

            performGetCardsByTopic(topic.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].term").value("cat"));
        }

        @Test
        @DisplayName("Get cards by topic: Fail because of topic not found")
        void testGetCardsByTopic_Fail_TopicNotFound() throws Exception {
            performGetCardsByTopic(99999)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get cards by topic: Fail because of missing token")
        void testGetCardsByTopic_Fail_Unauthenticated() throws Exception {
            mockMvc.perform(get("/flashcard/topics/{topicId}/cards", topic.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Get cards by topic: Fail because of invalid token")
        void testGetCardsByTopic_Fail_InvalidToken() throws Exception {
            mockMvc.perform(get("/flashcard/topics/{topicId}/cards", topic.getId())
                            .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("getQuizOptions")
    class GetQuizOptionsTests {
        private Deck deck;
        private Topic topic;

        @BeforeEach
        void setUp() {
            deck = persistDeck("Quiz Deck", "quiz-deck");
            topic = persistTopic(deck, "Quiz Topic", "quiz-topic");
        }

        /**
         * Persist 4 standard fruit cards; returns the target card (apple).
         */
        private Card persistFourFruitCards() {
            Card target = persistCard(deck, topic, "apple", "qua tao", 1);
            persistCard(deck, topic, "banana", "qua chuoi", 2);
            persistCard(deck, topic, "cherry", "qua anh dao", 3);
            persistCard(deck, topic, "mango", "qua xoai", 4);
            return target;
        }

        private ResultActions performGetQuizOptions(long topicId, long cardId) throws Exception {
            return mockMvc.perform(get("/flashcard/topics/{topicId}/cards/{cardId}/quiz-options",
                    topicId, cardId)
                    .header("Authorization", "Bearer " + authToken));
        }

        @Test
        @DisplayName("Get quiz options: Success")
        void testGetQuizOptions_Success() throws Exception {
            Card target = persistFourFruitCards();

            performGetQuizOptions(topic.getId(), target.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.options").isArray())
                    .andExpect(jsonPath("$.data.options.length()").value(4));
        }

        @Test
        @DisplayName("Get quiz options: Success - exactly one correct option")
        void testGetQuizOptions_Success_ExactlyOneCorrectOption() throws Exception {
            Card target = persistFourFruitCards();

            performGetQuizOptions(topic.getId(), target.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.options[?(@.isCorrect == true)]").value(hasSize(1)))
                    .andExpect(jsonPath("$.data.options[?(@.isCorrect == false)]").value(hasSize(3)));
        }

        @Test
        @DisplayName("Get quiz options: Success - correct option matches target term")
        void testGetQuizOptions_Success_CorrectOptionMatchesTargetTerm() throws Exception {
            Card target = persistFourFruitCards();

            performGetQuizOptions(topic.getId(), target.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.options[?(@.isCorrect == true)].term")
                            //?(@.isCorrect == true): lọc ra các phần tử trong mảng thỏa isCorrect == true
                            .value("apple"));
        }

        @Test
        @DisplayName("Get quiz options: Success - fallback to distractors from deck")
        void testGetQuizOptions_Success_FallbackToDistractorsFromDeck() throws Exception {
            Topic otherTopic = persistTopic(deck, "Fruits Extra", "fruits-extra");
            Card target = persistCard(deck, topic, "apple", "qua tao", 1);
            persistCard(deck, otherTopic, "pear", "qua le", 1);
            persistCard(deck, otherTopic, "peach", "qua dao", 2);
            persistCard(deck, otherTopic, "plum", "qua man", 3);

            performGetQuizOptions(topic.getId(), target.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.options.length()").value(4));
        }

        @Test
        @DisplayName("Get quiz options: Fail because of not enough cards")
        void testGetQuizOptions_Fail_NotEnoughCards() throws Exception {
            Card target = persistCard(deck, topic, "apple", "qua tao", 1);

            performGetQuizOptions(topic.getId(), target.getId())
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("QUIZ_NOT_ENOUGH_CARDS"));
        }

        @Test
        @DisplayName("Get quiz options: Fail because of card not found")
        void testGetQuizOptions_Fail_CardNotFound() throws Exception {
            performGetQuizOptions(topic.getId(), 99999)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("CARD_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get quiz options: Fail because of card not belong to topic")
        void testGetQuizOptions_Fail_CardNotBelongToTopic() throws Exception {
            Topic anotherTopic = persistTopic(deck, "Other", "other-" + System.nanoTime());
            Card cardInOtherTopic = persistCard(deck, anotherTopic, "apple", "qua tao", 1);

            performGetQuizOptions(topic.getId(), cardInOtherTopic.getId())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("CARD_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get quiz options: Fail because of topic not found")
        void testGetQuizOptions_Fail_TopicNotFound() throws Exception {
            performGetQuizOptions(99999, 1)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get quiz options: Fail because of missing token")
        void testGetQuizOptions_Fail_Unauthenticated() throws Exception {
            Card target = persistCard(deck, topic, "apple", "qua tao", 1);

            mockMvc.perform(get("/flashcard/topics/{topicId}/cards/{cardId}/quiz-options",
                            topic.getId(), target.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("submitSrsReview")
    class SubmitSrsReviewTests {
        private Deck deck;
        private Topic topic;
        private Card card;

        @BeforeEach
        void setUp() {
            deck = persistDeck("SRS Deck", "srs-deck");
            topic = persistTopic(deck, "SRS Topic", "srs-topic");
            card = persistCard(deck, topic, "hello", "xin chao", 1);
        }

        private void persistExistingState(int interval, BigDecimal easeFactor) {
            userCardStateRepository.save(
                    UserCardState.builder()
                            .user(testUser).card(card).deck(deck).topic(topic)
                            .srsInterval(interval)
                            .srsEaseFactor(easeFactor)
                            .build());
        }

        /**
         * PATCH /flashcard/{cardId}/srs with valid auth and a numeric grade.
         */
        private ResultActions performSrsReview(int grade) throws Exception {
            return mockMvc.perform(patch("/flashcard/{cardId}/srs", card.getId())
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"grade\": " + grade + "}"));
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade AGAIN creates new state with interval zero and decreased EF")
        void testSubmitSrsReview_Again_NewState_IntervalZero() throws Exception {
            performSrsReview(0)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId())
                    .orElseThrow(() -> new AssertionError("UserCardState must be created"));

            assertThat(state.getSrsInterval()).isEqualTo(0);
            assertThat(state.getSrsLastGrade()).isEqualTo((short) 0);

            // EF ban dau = 2.5, again-ef-penalty = -0.32 -> 2.18
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.18"));
            // isEqualByComparingTo: so sánh giá trị toán học 2.18 = 2.180

            assertThat(state.getSrsNextReviewAt())
                    .isAfter(LocalDateTime.now())
                    .isBefore(LocalDateTime.now().plusMinutes(15));
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade HARD creates new state with interval one and decreased EF")
        void testSubmitSrsReview_Hard_NewState_IntervalOne() throws Exception {
            performSrsReview(1)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId())
                    .orElseThrow();

            assertThat(state.getSrsInterval()).isEqualTo(1);
            assertThat(state.getSrsLastGrade()).isEqualTo((short) 1);

            // EF ban dau = 2.5, hard-ef-penalty = -0.14 -> 2.36
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.36"));

            assertThat(state.getSrsNextReviewAt().toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
            assertThat(state.getSrsNextReviewAt().toLocalDate())
                    .isEqualTo(LocalDate.now().plusDays(1));
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade GOOD creates new state with interval three and kept EF")
        void testSubmitSrsReview_Good_NewState_IntervalThree() throws Exception {
            performSrsReview(2)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId())
                    .orElseThrow();

            assertThat(state.getSrsInterval()).isEqualTo(3);
            assertThat(state.getSrsLastGrade()).isEqualTo((short) 2);

            // EF ban dau = 2.5
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.5"));

            assertThat(state.getSrsNextReviewAt().toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
            assertThat(state.getSrsNextReviewAt().toLocalDate())
                    .isEqualTo(LocalDate.now().plusDays(3));
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade EASY creates new state with interval five and increased EF")
        void testSubmitSrsReview_Easy_NewState_IntervalFive_EFIncreased() throws Exception {
            performSrsReview(3)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId())
                    .orElseThrow();

            assertThat(state.getSrsInterval()).isEqualTo(5);
            assertThat(state.getSrsLastGrade()).isEqualTo((short) 3);

            // EF ban dau = 2.5, easy-ef-bonus = +0.1 -> 2.6
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.6"));

            assertThat(state.getSrsNextReviewAt().toLocalDate())
                    .isEqualTo(LocalDate.now().plusDays(5));
        }

        @Test
        @DisplayName("Submit SRS review: Success - update existing state without duplicate")
        void testSubmitSrsReview_UpdatesExistingState_NoDuplicate() throws Exception {
            persistExistingState(3, new BigDecimal("2.50"));

            // GOOD(2) lan 2: prevInterval=3, newInterval = round half up(3 * 2.50) = 8
            performSrsReview(2)
                    .andExpect(status().isOk());

            assertThat(userCardStateRepository.count()).isEqualTo(1);
            UserCardState updated = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId()).orElseThrow();
            assertThat(updated.getSrsInterval()).isEqualTo(8);
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade AGAIN after learned resets interval and reduces EF")
        void testSubmitSrsReview_Again_AfterLearned_ResetsIntervalAndReducesEF() throws Exception {
            persistExistingState(3, new BigDecimal("2.50"));

            performSrsReview(0)
                    .andExpect(status().isOk());

            UserCardState updated = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId()).orElseThrow();

            assertThat(updated.getSrsInterval()).isEqualTo(0);

            // EF: 2.50 - 0.32 = 2.18
            assertThat(updated.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.18"));
        }

        @Test
        @DisplayName("Submit SRS review: Success - EF never drops below minimum")
        void testSubmitSrsReview_Again_EFNeverDropsBelowMinimum() throws Exception {
            persistExistingState(0, new BigDecimal("1.35"));

            performSrsReview(0)
                    .andExpect(status().isOk());

            UserCardState updated = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId()).orElseThrow();

            assertThat(updated.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("1.3"));
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade HARD after learned applies hard interval factor")
        void testSubmitSrsReview_Hard_AfterLearned_AppliesHardIntervalFactor() throws Exception {
            persistExistingState(5, new BigDecimal("2.50"));

            performSrsReview(1)
                    .andExpect(status().isOk());

            UserCardState updated = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId()).orElseThrow();

            // round hafl up(5 * 1.2) = 6
            assertThat(updated.getSrsInterval()).isEqualTo(6);
            // EF: 2.50 - 0.14 = 2.36
            assertThat(updated.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.36"));
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade EASY after learned applies easy interval factor")
        void testSubmitSrsReview_Easy_AfterLearned_AppliesEasyIntervalFactor() throws Exception {
            persistExistingState(5, new BigDecimal("2.50"));

            performSrsReview(3)
                    .andExpect(status().isOk());

            UserCardState updated = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId()).orElseThrow();

            // newEF = 2.5 + 0.1 = 2.6
            assertThat(updated.getSrsEaseFactor()).isEqualByComparingTo("2.6");
            // newInterval = round half up(5 * 2.6 * 1.3) = 17
            assertThat(updated.getSrsInterval()).isEqualTo(17);
        }

        @Test
        @DisplayName("Submit SRS review: Fail because of null grade")
        void testSubmitSrsReview_Fail_NullGrade() throws Exception {
            mockMvc.perform(patch("/flashcard/{cardId}/srs", card.getId())
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grade\": null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_SRS_GRADE"));
        }

        @Test
        @DisplayName("Submit SRS review: Fail because of grade too high")
        void testSubmitSrsReview_Fail_GradeTooHigh() throws Exception {
            performSrsReview(4)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_SRS_GRADE"));
        }

        @Test
        @DisplayName("Submit SRS review: Fail because of negative grade")
        void testSubmitSrsReview_Fail_GradeNegative() throws Exception {
            performSrsReview(-1)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_SRS_GRADE"));
        }

        @Test
        @DisplayName("Submit SRS review: Fail because of empty body")
        void testSubmitSrsReview_Fail_EmptyBody() throws Exception {
            mockMvc.perform(patch("/flashcard/{cardId}/srs", card.getId())
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_SRS_GRADE"));
        }
    }
}
