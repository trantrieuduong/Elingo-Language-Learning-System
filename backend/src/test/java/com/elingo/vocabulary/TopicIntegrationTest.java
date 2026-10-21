package com.elingo.vocabulary;

import com.elingo.BaseIntegrationTest;
import com.elingo.auth.service.JwtService;
import com.elingo.learning.flashcard.entity.UserCardState;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.dto.request.CreateUserTopicRequest;
import com.elingo.vocabulary.dto.request.UpdateUserTopicRequest;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Autowired
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        authToken = jwtService.generateAccessToken(testUser.getId().toString(), testUser.getRole().name());
    }

    // ─────────────────────── helpers ───────────────────────

    private Deck persistDeck() {
        return deckRepository.save(Deck.builder()
                .title("Animals Deck")
                .slug("animals-deck")
                .status(DeckStatus.PUBLISHED)
                .ownerType(OwnerType.SYSTEM)
                .publishedAt(LocalDateTime.now())
                .build());
    }

    private Deck persistUserDeck(User owner, String title, String slug, DeckStatus status) {
        return deckRepository.save(Deck.builder()
                .title(title)
                .slug(slug)
                .status(status)
                .ownerType(OwnerType.USER)
                .owner(owner)
                .publishedAt(status == DeckStatus.PUBLISHED ? LocalDateTime.now() : null)
                .build());
    }

    private Topic persistTopic(Deck deck) {
        return topicRepository.save(Topic.builder()
                .deck(deck)
                .name("Animals")
                .slug("animals")
                .order(1)
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

    private void persistUserCardState(User user, Card card, Short lastGrade, Boolean starred, Boolean hidden) {
        userCardStateRepository.save(UserCardState.builder()
                .user(user)
                .card(card)
                .srsLastGrade(lastGrade)
                .flagsStarred(starred)
                .flagsHidden(hidden)
                .build());
    }

    private ResultActions performGetCardsByTopic(long topicId) throws Exception {
        return mockMvc.perform(get("/topics/{topicId}/cards", topicId)
                .header("Authorization", "Bearer " + authToken));
    }

    private ResultActions performPostTopic(Object body) throws Exception {
        return mockMvc.perform(post("/topics")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performPutTopic(Long topicId, Object body) throws Exception {
        return mockMvc.perform(put("/topics/{topicId}", topicId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performDeleteTopic(Long topicId) throws Exception {
        return mockMvc.perform(delete("/topics/{topicId}", topicId)
                .header("Authorization", "Bearer " + authToken));
    }

    // ─────────────────────── getCardsByTopic ───────────────────────

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

        @Test
        @DisplayName("Get cards by topic: Success - returns all cards with initial learning states")
        void testGetCardsByTopic_Success() throws Exception {
            persistCard(deck, topic, "cat", "con meo", 1);
            persistCard(deck, topic, "dog", "con cho", 2);

            performGetCardsByTopic(topic.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].term").value("cat"))
                    .andExpect(jsonPath("$.data[0].state.learned").value(false))
                    .andExpect(jsonPath("$.data[0].state.starred").value(false))
                    .andExpect(jsonPath("$.data[0].state.hidden").value(false))
                    .andExpect(jsonPath("$.data[1].term").value("dog"))
                    .andExpect(jsonPath("$.data[1].state.learned").value(false))
                    .andExpect(jsonPath("$.data[1].state.starred").value(false))
                    .andExpect(jsonPath("$.data[1].state.hidden").value(false));
        }

        @Test
        @DisplayName("Get cards by topic: Success - correctly reflects learned, starred, and hidden states")
        void testGetCardsByTopic_ReturnsCardStates() throws Exception {
            Card card1 = persistCard(deck, topic, "cat", "con meo", 1);
            Card card2 = persistCard(deck, topic, "dog", "con cho", 2);
            Card card3 = persistCard(deck, topic, "bird", "con chim", 3);

            persistUserCardState(testUser, card1, (short) 3, false, false);
            persistUserCardState(testUser, card2, null, true, false);
            persistUserCardState(testUser, card3, null, false, true);

            performGetCardsByTopic(topic.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0].term").value("cat"))
                    .andExpect(jsonPath("$.data[0].state.learned").value(true))
                    .andExpect(jsonPath("$.data[0].state.starred").value(false))
                    .andExpect(jsonPath("$.data[0].state.hidden").value(false))
                    .andExpect(jsonPath("$.data[1].term").value("dog"))
                    .andExpect(jsonPath("$.data[1].state.learned").value(false))
                    .andExpect(jsonPath("$.data[1].state.starred").value(true))
                    .andExpect(jsonPath("$.data[1].state.hidden").value(false))
                    .andExpect(jsonPath("$.data[2].term").value("bird"))
                    .andExpect(jsonPath("$.data[2].state.learned").value(false))
                    .andExpect(jsonPath("$.data[2].state.starred").value(false))
                    .andExpect(jsonPath("$.data[2].state.hidden").value(true));
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
        @DisplayName("Get cards by topic: Success - other user state does not affect current user state")
        void testGetCardsByTopic_OtherUserStateDoesNotAffectCurrentUser() throws Exception {
            Card card = persistCard(deck, topic, "cat", "con meo", 1);

            User otherUser = userRepository.save(User.builder()
                    .username("other_user_fc")
                    .email("other_fc@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other User")
                    .isActive(true)
                    .isVerified(true)
                    .build());
            persistUserCardState(otherUser, card, (short) 4, true, false);

            performGetCardsByTopic(topic.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].term").value("cat"))
                    .andExpect(jsonPath("$.data[0].state.learned").value(false))
                    .andExpect(jsonPath("$.data[0].state.starred").value(false));
        }

        @Test
        @DisplayName("Get cards by topic: Success - owner can view cards in own draft deck")
        void testGetCardsByTopic_Success_OwnerCanViewCardsInDraftDeck() throws Exception {
            Deck userDraftDeck = persistUserDeck(testUser, "Draft Deck", "draft-deck", DeckStatus.DRAFT);
            Topic userTopic = persistTopic(userDraftDeck);
            persistCard(userDraftDeck, userTopic, "fish", "con ca", 1);

            performGetCardsByTopic(userTopic.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].term").value("fish"));
        }

        @Test
        @DisplayName("Get cards by topic: Fail because deck is draft of another user")
        void testGetCardsByTopic_Fail_DraftDeckNotOwner() throws Exception {
            Deck draftDeck = deckRepository.save(Deck.builder()
                    .title("Draft Deck")
                    .slug("draft-deck-sys")
                    .status(DeckStatus.DRAFT)
                    .ownerType(OwnerType.SYSTEM)
                    .build());
            Topic draftTopic = persistTopic(draftDeck);

            performGetCardsByTopic(draftTopic.getId())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get cards by topic: Fail because deck is premium and user has no subscription")
        void testGetCardsByTopic_Fail_PremiumDeckWithoutSubscription() throws Exception {
            Deck premiumDeck = deckRepository.save(Deck.builder()
                    .title("Premium Deck")
                    .slug("premium-deck")
                    .status(DeckStatus.PUBLISHED)
                    .ownerType(OwnerType.SYSTEM)
                    .isPremium(true)
                    .publishedAt(LocalDateTime.now())
                    .build());
            Topic premiumTopic = persistTopic(premiumDeck);

            performGetCardsByTopic(premiumTopic.getId())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("PREMIUM_REQUIRED"));
        }

        @Test
        @DisplayName("Get cards by topic: Fail because topic not found")
        void testGetCardsByTopic_Fail_TopicNotFound() throws Exception {
            performGetCardsByTopic(99999L)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get cards by topic: Fail because missing token")
        void testGetCardsByTopic_Fail_Unauthenticated() throws Exception {
            mockMvc.perform(get("/topics/{topicId}/cards", topic.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Get cards by topic: Fail because invalid token")
        void testGetCardsByTopic_Fail_InvalidToken() throws Exception {
            mockMvc.perform(get("/topics/{topicId}/cards", topic.getId())
                            .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────── createUserTopic ───────────────────────

    @Nested
    @DisplayName("createUserTopic")
    class CreateUserTopicTests {

        @Test
        @DisplayName("Create user topic: Success - creates topic with order 1 and increments deck topicCount")
        void testCreateUserTopic_Success() throws Exception {
            Deck userDeck = persistUserDeck(testUser, "Grammar Deck", "grammar-deck", DeckStatus.DRAFT);

            CreateUserTopicRequest req = new CreateUserTopicRequest(userDeck.getId(), "Tenses");

            performPostTopic(req)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Tenses"))
                    .andExpect(jsonPath("$.data.slug").value(startsWith("tenses-")))
                    .andExpect(jsonPath("$.data.order").value(1));

            entityManager.flush();
            entityManager.clear();
            Deck updatedDeck = deckRepository.findById(userDeck.getId()).orElseThrow();
            assertThat(updatedDeck.getTopicCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Create user topic: Success - auto increments order for subsequent topics")
        void testCreateUserTopic_Success_AutoIncrementsOrder() throws Exception {
            Deck userDeck = persistUserDeck(testUser, "Grammar Deck 2", "grammar-deck-2", DeckStatus.DRAFT);

            performPostTopic(new CreateUserTopicRequest(userDeck.getId(), "Topic One"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.order").value(1));

            performPostTopic(new CreateUserTopicRequest(userDeck.getId(), "Topic Two"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.order").value(2));

            entityManager.flush();
            entityManager.clear();
            Deck updatedDeck = deckRepository.findById(userDeck.getId()).orElseThrow();
            assertThat(updatedDeck.getTopicCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Create user topic: Fail because deck not found")
        void testCreateUserTopic_Fail_DeckNotFound() throws Exception {
            CreateUserTopicRequest req = new CreateUserTopicRequest(99999L, "Tenses");

            performPostTopic(req)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Create user topic: Fail because deck belongs to another user")
        void testCreateUserTopic_Fail_NotOwner() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .username("other_user_topic")
                    .email("other_topic@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other User")
                    .isActive(true)
                    .isVerified(true)
                    .build());
            Deck otherDeck = persistUserDeck(otherUser, "Other Deck", "other-deck", DeckStatus.PUBLISHED);

            CreateUserTopicRequest req = new CreateUserTopicRequest(otherDeck.getId(), "Hacked Topic");

            performPostTopic(req)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Create user topic: Fail because topic limit exceeded (max 15)")
        void testCreateUserTopic_Fail_TopicLimitExceeded() throws Exception {
            Deck userDeck = persistUserDeck(testUser, "Big Deck", "big-deck", DeckStatus.DRAFT);
            for (int i = 1; i <= 15; i++) {
                topicRepository.save(Topic.builder()
                        .deck(userDeck)
                        .name("Topic " + i)
                        .slug("topic-" + i)
                        .order(i)
                        .build());
            }

            CreateUserTopicRequest req = new CreateUserTopicRequest(userDeck.getId(), "Topic 16");

            performPostTopic(req)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_LIMIT_EXCEEDED"));
        }

        @Test
        @DisplayName("Create user topic: Fail because name is blank")
        void testCreateUserTopic_Fail_BlankName() throws Exception {
            Deck userDeck = persistUserDeck(testUser, "Deck", "deck-blank", DeckStatus.DRAFT);

            CreateUserTopicRequest req = new CreateUserTopicRequest(userDeck.getId(), "   ");

            performPostTopic(req)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Create user topic: Fail because unauthenticated")
        void testCreateUserTopic_Fail_Unauthenticated() throws Exception {
            mockMvc.perform(post("/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateUserTopicRequest(1L, "Topic"))))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────── updateUserTopic ───────────────────────

    @Nested
    @DisplayName("updateUserTopic")
    class UpdateUserTopicTests {

        @Test
        @DisplayName("Update user topic: Success")
        void testUpdateUserTopic_Success() throws Exception {
            Deck userDeck = persistUserDeck(testUser, "Grammar Deck", "grammar-deck-up", DeckStatus.DRAFT);
            Topic userTopic = persistTopic(userDeck);

            UpdateUserTopicRequest req = new UpdateUserTopicRequest("Advanced Grammar");

            performPutTopic(userTopic.getId(), req)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Advanced Grammar"));

            entityManager.flush();
            entityManager.clear();
            Topic updated = topicRepository.findById(userTopic.getId()).orElseThrow();
            assertThat(updated.getName()).isEqualTo("Advanced Grammar");
        }

        @Test
        @DisplayName("Update user topic: Fail because topic not found")
        void testUpdateUserTopic_Fail_NotFound() throws Exception {
            UpdateUserTopicRequest req = new UpdateUserTopicRequest("New Name");

            performPutTopic(99999L, req)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }

        @Test
        @DisplayName("Update user topic: Fail because topic belongs to another user")
        void testUpdateUserTopic_Fail_NotOwner() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .username("other_user_topic_2")
                    .email("other_topic2@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other User 2")
                    .isActive(true)
                    .isVerified(true)
                    .build());
            Deck otherDeck = persistUserDeck(otherUser, "Other Deck", "other-deck-up", DeckStatus.PUBLISHED);
            Topic otherTopic = persistTopic(otherDeck);

            UpdateUserTopicRequest req = new UpdateUserTopicRequest("Hacked Topic Name");

            performPutTopic(otherTopic.getId(), req)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Update user topic: Fail because name is blank")
        void testUpdateUserTopic_Fail_BlankName() throws Exception {
            Deck userDeck = persistUserDeck(testUser, "Grammar Deck", "grammar-blank", DeckStatus.DRAFT);
            Topic userTopic = persistTopic(userDeck);

            UpdateUserTopicRequest req = new UpdateUserTopicRequest("");

            performPutTopic(userTopic.getId(), req)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }
    }

    // ─────────────────────── deleteUserTopic ───────────────────────

    @Nested
    @DisplayName("deleteUserTopic")
    class DeleteUserTopicTests {

        @Test
        @DisplayName("Delete user topic: Success - detaches cards and decrements topic and card counts")
        void testDeleteUserTopic_Success_DetachesCardsAndAdjustsCounts() throws Exception {
            Deck userDeck = persistUserDeck(testUser, "Deck With Cards", "deck-with-cards", DeckStatus.DRAFT);
            userDeck.setTopicCount(1);
            userDeck.setCardCount(2);
            deckRepository.save(userDeck);

            Topic topic = persistTopic(userDeck);
            Card card1 = persistCard(userDeck, topic, "cat", "con meo", 1);
            Card card2 = persistCard(userDeck, topic, "dog", "con cho", 2);

            performDeleteTopic(topic.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            assertThat(topicRepository.findById(topic.getId())).isEmpty();

            entityManager.flush();
            entityManager.clear();

            Card detached1 = cardRepository.findById(card1.getId()).orElseThrow();
            assertThat(detached1.getTopic()).isNull();
            assertThat(detached1.getDeck()).isNull();

            Card detached2 = cardRepository.findById(card2.getId()).orElseThrow();
            assertThat(detached2.getTopic()).isNull();
            assertThat(detached2.getDeck()).isNull();

            Deck updatedDeck = deckRepository.findById(userDeck.getId()).orElseThrow();
            assertThat(updatedDeck.getTopicCount()).isEqualTo(0);
            assertThat(updatedDeck.getCardCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Delete user topic: Fail because topic not found")
        void testDeleteUserTopic_Fail_NotFound() throws Exception {
            performDeleteTopic(99999L)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }

        @Test
        @DisplayName("Delete user topic: Fail because not owner")
        void testDeleteUserTopic_Fail_NotOwner() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .username("other_user_del")
                    .email("other_del@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other User Del")
                    .isActive(true)
                    .isVerified(true)
                    .build());
            Deck otherDeck = persistUserDeck(otherUser, "Other Deck", "other-del-deck", DeckStatus.PUBLISHED);
            Topic otherTopic = persistTopic(otherDeck);

            performDeleteTopic(otherTopic.getId())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }
    }
}
