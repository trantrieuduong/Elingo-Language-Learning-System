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

    private Deck persistDeck() {
        return deckRepository.save(Deck.builder()
                .title("Animals Deck")
                .slug("animals-deck")
                .status(DeckStatus.PUBLISHED)
                .ownerType(OwnerType.SYSTEM)
                .build());
    }

    private Topic persistTopic(Deck deck) {
        return topicRepository.save(Topic.builder()
                .deck(deck)
                .name("Animals")
                .slug("animals")
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
            deck = persistDeck();
            topic = persistTopic(deck);
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
}