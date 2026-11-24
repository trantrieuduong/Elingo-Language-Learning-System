package com.elingo.admin.content;

import com.elingo.BaseIntegrationTest;
import com.elingo.vocabulary.dto.request.CreateSystemTopicRequest;
import com.elingo.vocabulary.dto.request.ReorderItemRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemTopicRequest;
import com.elingo.auth.service.JwtService;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.entity.*;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AdminTopicIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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

    private User adminUser;
    private String adminToken;

    private User regularUser;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .username("admin_topic_user")
                .email("admin_topic@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Admin Topic")
                .isActive(true)
                .isVerified(true)
                .role(Role.ADMIN)
                .build());
        adminToken = jwtService.generateAccessToken(adminUser.getId().toString(), adminUser.getRole().name());

        regularUser = userRepository.save(User.builder()
                .username("regular_topic_user")
                .email("regular_topic@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Regular User")
                .isActive(true)
                .isVerified(true)
                .role(Role.USER)
                .build());
        userToken = jwtService.generateAccessToken(regularUser.getId().toString(), regularUser.getRole().name());
    }

    // ─────────────────────── helpers ───────────────────────

    private Deck persistSystemDeck(String title, String slug) {
        return deckRepository.save(Deck.builder()
                .title(title)
                .slug(slug)
                .status(DeckStatus.PUBLISHED)
                .ownerType(OwnerType.SYSTEM)
                .owner(null)
                .cardCount(0)
                .topicCount(0)
                .publishedAt(LocalDateTime.now())
                .build());
    }

    private Deck persistUserDeck(User owner, String title, String slug) {
        return deckRepository.save(Deck.builder()
                .title(title)
                .slug(slug)
                .status(DeckStatus.DRAFT)
                .ownerType(OwnerType.USER)
                .owner(owner)
                .cardCount(0)
                .topicCount(0)
                .build());
    }

    private Topic persistTopic(Deck deck, String name, String slug, int order) {
        return topicRepository.save(Topic.builder()
                .deck(deck)
                .name(name)
                .slug(slug)
                .order(order)
                .cardCount(0)
                .build());
    }

    private Card persistCard(Deck deck, Topic topic, String term, int order) {
        return cardRepository.save(Card.builder()
                .deck(deck)
                .topic(topic)
                .term(term)
                .translation("Nghĩa")
                .order(order)
                .build());
    }

    // ─────────────────────── test suites ───────────────────────

    @Nested
    @DisplayName("createSystemTopic")
    class CreateSystemTopicTests {

        @Test
        @DisplayName("Create system topic: Success")
        void testCreateSystemTopic_Success() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "system-deck-topic-create");

            CreateSystemTopicRequest request = new CreateSystemTopicRequest(deck.getId(), "Grammar Topic");

            mockMvc.perform(post("/admin/topics")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Grammar Topic"));

            entityManager.flush();
            entityManager.clear();
            Deck reloadedDeck = deckRepository.findById(deck.getId()).orElseThrow();
            assertThat(reloadedDeck.getTopicCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Create system topic: Fail when deck is USER owned")
        void testCreateSystemTopic_Fail_UserDeck() throws Exception {
            Deck userDeck = persistUserDeck(regularUser, "User Deck", "user-deck-topic-create");

            CreateSystemTopicRequest request = new CreateSystemTopicRequest(userDeck.getId(), "Topic Name");

            mockMvc.perform(post("/admin/topics")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_SYSTEM"));
        }

        @Test
        @DisplayName("Create system topic: Fail when deck not found")
        void testCreateSystemTopic_Fail_DeckNotFound() throws Exception {
            CreateSystemTopicRequest request = new CreateSystemTopicRequest(99999L, "Topic Name");

            mockMvc.perform(post("/admin/topics")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Create system topic: Fail when role is USER")
        void testCreateSystemTopic_Fail_Forbidden_UserRole() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "sys-deck-auth");

            CreateSystemTopicRequest request = new CreateSystemTopicRequest(deck.getId(), "Topic Name");

            mockMvc.perform(post("/admin/topics")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("updateSystemTopic")
    class UpdateSystemTopicTests {

        @Test
        @DisplayName("Update system topic: Success")
        void testUpdateSystemTopic_Success() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "system-deck-topic-update");
            Topic topic = persistTopic(deck, "Old Topic Name", "old-topic-name", 1);

            UpdateSystemTopicRequest request = new UpdateSystemTopicRequest("New Topic Name");

            mockMvc.perform(put("/admin/topics/{topicId}", topic.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("New Topic Name"));
        }

        @Test
        @DisplayName("Update system topic: Fail when topic belongs to USER deck")
        void testUpdateSystemTopic_Fail_UserTopic() throws Exception {
            Deck userDeck = persistUserDeck(regularUser, "User Deck", "user-deck-top-upd");
            Topic topic = persistTopic(userDeck, "User Topic", "user-topic", 1);

            UpdateSystemTopicRequest request = new UpdateSystemTopicRequest("Renamed Topic");

            mockMvc.perform(put("/admin/topics/{topicId}", topic.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_SYSTEM"));
        }

        @Test
        @DisplayName("Update system topic: Fail when topic not found")
        void testUpdateSystemTopic_Fail_NotFound() throws Exception {
            UpdateSystemTopicRequest request = new UpdateSystemTopicRequest("Renamed Topic");

            mockMvc.perform(put("/admin/topics/{topicId}", 99999L)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("deleteSystemTopic")
    class DeleteSystemTopicTests {

        @Test
        @DisplayName("Delete system topic: Success detaches cards and deletes topic")
        void testDeleteSystemTopic_Success() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "sys-deck-topic-del");
            Topic topic = persistTopic(deck, "Topic to Delete", "del-topic", 1);
            Card card = persistCard(deck, topic, "Hello", 1);

            mockMvc.perform(delete("/admin/topics/{topicId}", topic.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            entityManager.flush();
            entityManager.clear();
            assertThat(topicRepository.findById(topic.getId())).isEmpty();
            Card detachedCard = cardRepository.findById(card.getId()).orElseThrow();
            assertThat(detachedCard.getTopic()).isNull();
            assertThat(detachedCard.getDeck()).isNull();
        }

        @Test
        @DisplayName("Delete system topic: Fail when topic belongs to USER deck")
        void testDeleteSystemTopic_Fail_UserTopic() throws Exception {
            Deck userDeck = persistUserDeck(regularUser, "User Deck", "user-deck-top-del");
            Topic topic = persistTopic(userDeck, "User Topic", "user-topic-del", 1);

            mockMvc.perform(delete("/admin/topics/{topicId}", topic.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_SYSTEM"));
        }

        @Test
        @DisplayName("Delete system topic: Fail when topic not found")
        void testDeleteSystemTopic_Fail_NotFound() throws Exception {
            mockMvc.perform(delete("/admin/topics/{topicId}", 99999L)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("getCardsByTopicId")
    class GetCardsByTopicIdTests {

        @Test
        @DisplayName("Get cards by topic ID: Success")
        void testGetCardsByTopicId_Success() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "sys-deck-cards");
            Topic topic = persistTopic(deck, "Topic A", "topic-a-cards", 1);
            persistCard(deck, topic, "Card 1", 1);
            persistCard(deck, topic, "Card 2", 2);

            mockMvc.perform(get("/admin/topics/{topicId}/cards", topic.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @DisplayName("Get cards by topic ID: Fail when topic not found")
        void testGetCardsByTopicId_Fail_NotFound() throws Exception {
            mockMvc.perform(get("/admin/topics/{topicId}/cards", 99999L)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("reorderSystemCards")
    class ReorderSystemCardsTests {

        @Test
        @DisplayName("Reorder system cards: Success")
        void testReorderSystemCards_Success() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "sys-deck-reorder-cards");
            Topic topic = persistTopic(deck, "Topic Reorder", "top-reorder-cards", 1);
            Card c1 = persistCard(deck, topic, "C1", 1);
            Card c2 = persistCard(deck, topic, "C2", 2);

            ReorderItemRequest request = new ReorderItemRequest(List.of(c2.getId(), c1.getId()));

            mockMvc.perform(patch("/admin/topics/{topicId}/cards/orders", topic.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            entityManager.flush();
            entityManager.clear();
            Card reloaded1 = cardRepository.findById(c1.getId()).orElseThrow();
            Card reloaded2 = cardRepository.findById(c2.getId()).orElseThrow();
            assertThat(reloaded2.getOrder()).isEqualTo(1);
            assertThat(reloaded1.getOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("Reorder system cards: Fail when card does not belong to topic")
        void testReorderSystemCards_Fail_CardMismatch() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "sys-deck-reorder-mismatch");
            Topic topic1 = persistTopic(deck, "Topic 1", "t1-mismatch", 1);
            Topic topic2 = persistTopic(deck, "Topic 2", "t2-mismatch", 2);
            Card cardInTopic2 = persistCard(deck, topic2, "C2", 1);

            ReorderItemRequest request = new ReorderItemRequest(List.of(cardInTopic2.getId()));

            mockMvc.perform(patch("/admin/topics/{topicId}/cards/orders", topic1.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }
    }
}
