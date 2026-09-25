package com.elingo.vocabulary;

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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicIntegrationTest extends BaseIntegrationTest {
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
                .username("topic_tester")
                .email("topic_tester@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Topic Tester")
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
            return mockMvc.perform(get("/topics/{topicId}/cards", topicId)
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
            mockMvc.perform(get("/topics/{topicId}/cards", topic.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Get cards by topic: Fail because of invalid token")
        void testGetCardsByTopic_Fail_InvalidToken() throws Exception {
            mockMvc.perform(get("/topics/{topicId}/cards", topic.getId())
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
            return mockMvc.perform(get("/topics/{topicId}/cards/{cardId}/quiz-options",
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
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(4));
        }

        @Test
        @DisplayName("Get quiz options: Success - exactly one correct option")
        void testGetQuizOptions_Success_ExactlyOneCorrectOption() throws Exception {
            Card target = persistFourFruitCards();

            performGetQuizOptions(topic.getId(), target.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[?(@.isCorrect == true)]").value(hasSize(1)))
                    .andExpect(jsonPath("$.data[?(@.isCorrect == false)]").value(hasSize(3)));
        }

        @Test
        @DisplayName("Get quiz options: Success - correct option matches target term")
        void testGetQuizOptions_Success_CorrectOptionMatchesTargetTerm() throws Exception {
            Card target = persistFourFruitCards();

            performGetQuizOptions(topic.getId(), target.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[?(@.isCorrect == true)].term")
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
                    .andExpect(jsonPath("$.data.length()").value(4));
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
            Topic anotherTopic = persistTopic(deck, "Other", "other");
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

            mockMvc.perform(get("/topics/{topicId}/cards/{cardId}/quiz-options",
                            topic.getId(), target.getId()))
                    .andExpect(status().isForbidden());
        }
    }
}