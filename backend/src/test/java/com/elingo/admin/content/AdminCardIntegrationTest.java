package com.elingo.admin.content;

import com.elingo.BaseIntegrationTest;
import com.elingo.vocabulary.dto.request.CardPhoneticRequest;
import com.elingo.vocabulary.dto.request.CreateSystemCardRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemCardRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AdminCardIntegrationTest extends BaseIntegrationTest {

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
                .username("admin_card_user")
                .email("admin_card@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Admin Card")
                .isActive(true)
                .isVerified(true)
                .role(Role.ADMIN)
                .build());
        adminToken = jwtService.generateAccessToken(adminUser.getId().toString(), adminUser.getRole().name());

        regularUser = userRepository.save(User.builder()
                .username("regular_card_user")
                .email("regular_card@elingo.test")
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

    private Topic persistTopic(Deck deck, String name, String slug) {
        return topicRepository.save(Topic.builder()
                .deck(deck)
                .name(name)
                .slug(slug)
                .order(1)
                .cardCount(0)
                .build());
    }

    private Card persistCard(Deck deck, Topic topic, String term, String translation) {
        return cardRepository.save(Card.builder()
                .deck(deck)
                .topic(topic)
                .term(term)
                .translation(translation)
                .order(1)
                .build());
    }

    // ─────────────────────── test suites ───────────────────────

    @Nested
    @DisplayName("getCardById")
    class GetCardByIdTests {

        @Test
        @DisplayName("Get card by ID: Success")
        void testGetCardById_Success() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "sys-deck-get-card");
            Topic topic = persistTopic(deck, "Topic 1", "top-get-card");
            Card card = persistCard(deck, topic, "abandon", "từ bỏ");

            mockMvc.perform(get("/admin/cards/{cardId}", card.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(card.getId()))
                    .andExpect(jsonPath("$.data.term").value("abandon"));
        }

        @Test
        @DisplayName("Get card by ID: Fail when card not found")
        void testGetCardById_Fail_NotFound() throws Exception {
            mockMvc.perform(get("/admin/cards/{cardId}", 99999L)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("CARD_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get card by ID: Fail when role is USER")
        void testGetCardById_Fail_Forbidden_UserRole() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "sys-deck-auth-card");
            Topic topic = persistTopic(deck, "Topic 1", "top-auth-card");
            Card card = persistCard(deck, topic, "term", "nghĩa");

            mockMvc.perform(get("/admin/cards/{cardId}", card.getId())
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("createSystemCard")
    class CreateSystemCardTests {

        @Test
        @DisplayName("Create system card: Success")
        void testCreateSystemCard_Success() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "sys-deck-card-create");
            Topic topic = persistTopic(deck, "Topic 1", "top-card-create");

            CreateSystemCardRequest request = new CreateSystemCardRequest(
                    topic.getId(),
                    "benevolent",
                    "adj",
                    "nhân từ, tốt bụng",
                    "rộng lượng, tử tế",
                    "well meaning and kindly",
                    "Ông ấy là người nhân từ.",
                    "He was a benevolent person.",
                    null,
                    List.of(new CardPhoneticRequest("/bəˈnev.əl.ənt/", null, "en-US"))
            );

            mockMvc.perform(post("/admin/cards")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.term").value("benevolent"))
                    .andExpect(jsonPath("$.data.translation").value("nhân từ, tốt bụng"))
                    .andExpect(jsonPath("$.data.phonetics").isArray());

            entityManager.flush();
            entityManager.clear();
            Topic reloadedTopic = topicRepository.findById(topic.getId()).orElseThrow();
            Deck reloadedDeck = deckRepository.findById(deck.getId()).orElseThrow();
            assertThat(reloadedTopic.getCardCount()).isEqualTo(1);
            assertThat(reloadedDeck.getCardCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Create system card: Fail when topic belongs to USER deck")
        void testCreateSystemCard_Fail_UserTopic() throws Exception {
            Deck userDeck = persistUserDeck(regularUser, "User Deck", "user-deck-card-create");
            Topic topic = persistTopic(userDeck, "User Topic", "user-top-card-create");

            CreateSystemCardRequest request = new CreateSystemCardRequest(
                    topic.getId(),
                    "term",
                    "noun",
                    "nghĩa",
                    null, null, null, null, null, null
            );

            mockMvc.perform(post("/admin/cards")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_SYSTEM"));
        }

        @Test
        @DisplayName("Create system card: Fail when topic not found")
        void testCreateSystemCard_Fail_TopicNotFound() throws Exception {
            CreateSystemCardRequest request = new CreateSystemCardRequest(
                    99999L,
                    "term",
                    "noun",
                    "nghĩa",
                    null, null, null, null, null, null
            );

            mockMvc.perform(post("/admin/cards")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }

        @Test
        @DisplayName("Create system card: Fail validation on missing term")
        void testCreateSystemCard_Fail_ValidationMissingTerm() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "sys-deck-card-val");
            Topic topic = persistTopic(deck, "Topic 1", "top-card-val");

            CreateSystemCardRequest request = new CreateSystemCardRequest(
                    topic.getId(),
                    "",
                    "noun",
                    "nghĩa",
                    null, null, null, null, null, null
            );

            mockMvc.perform(post("/admin/cards")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }
    }

    @Nested
    @DisplayName("updateSystemCard")
    class UpdateSystemCardTests {

        @Test
        @DisplayName("Update system card: Success")
        void testUpdateSystemCard_Success() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "sys-deck-card-update");
            Topic topic = persistTopic(deck, "Topic 1", "top-card-update");
            Card card = persistCard(deck, topic, "old term", "old meaning");

            UpdateSystemCardRequest request = new UpdateSystemCardRequest(
                    "new term",
                    "verb",
                    "new meaning",
                    "explanation vi",
                    "explanation en",
                    "example vi",
                    "example en",
                    null,
                    List.of(new CardPhoneticRequest("/nuː/", null, "en-US"))
            );

            mockMvc.perform(put("/admin/cards/{cardId}", card.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.term").value("new term"))
                    .andExpect(jsonPath("$.data.translation").value("new meaning"));
        }

        @Test
        @DisplayName("Update system card: Fail when card belongs to USER deck")
        void testUpdateSystemCard_Fail_UserCard() throws Exception {
            Deck userDeck = persistUserDeck(regularUser, "User Deck", "user-deck-card-upd");
            Topic topic = persistTopic(userDeck, "User Topic", "user-top-card-upd");
            Card card = persistCard(userDeck, topic, "user term", "user meaning");

            UpdateSystemCardRequest request = new UpdateSystemCardRequest(
                    "updated term",
                    "noun",
                    "updated meaning",
                    null, null, null, null, null, null
            );

            mockMvc.perform(put("/admin/cards/{cardId}", card.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_SYSTEM"));
        }

        @Test
        @DisplayName("Update system card: Fail when card not found")
        void testUpdateSystemCard_Fail_NotFound() throws Exception {
            UpdateSystemCardRequest request = new UpdateSystemCardRequest(
                    "term",
                    "noun",
                    "meaning",
                    null, null, null, null, null, null
            );

            mockMvc.perform(put("/admin/cards/{cardId}", 99999L)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("CARD_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("deleteSystemCard")
    class DeleteSystemCardTests {

        @Test
        @DisplayName("Delete system card: Success")
        void testDeleteSystemCard_Success() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "sys-deck-card-del");
            Topic topic = persistTopic(deck, "Topic 1", "top-card-del");
            Card card = persistCard(deck, topic, "delete me", "xóa tôi");
            topic.setCardCount(1);
            deck.setCardCount(1);
            topicRepository.save(topic);
            deckRepository.save(deck);

            mockMvc.perform(delete("/admin/cards/{cardId}", card.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            entityManager.flush();
            entityManager.clear();
            assertThat(cardRepository.findById(card.getId())).isEmpty();
            Topic reloadedTopic = topicRepository.findById(topic.getId()).orElseThrow();
            Deck reloadedDeck = deckRepository.findById(deck.getId()).orElseThrow();
            assertThat(reloadedTopic.getCardCount()).isEqualTo(0);
            assertThat(reloadedDeck.getCardCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Delete system card: Fail when card belongs to USER deck")
        void testDeleteSystemCard_Fail_UserCard() throws Exception {
            Deck userDeck = persistUserDeck(regularUser, "User Deck", "user-deck-card-del");
            Topic topic = persistTopic(userDeck, "User Topic", "user-top-card-del");
            Card card = persistCard(userDeck, topic, "user card", "nghĩa");

            mockMvc.perform(delete("/admin/cards/{cardId}", card.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_SYSTEM"));
        }

        @Test
        @DisplayName("Delete system card: Fail when card not found")
        void testDeleteSystemCard_Fail_NotFound() throws Exception {
            mockMvc.perform(delete("/admin/cards/{cardId}", 99999L)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("CARD_NOT_FOUND"));
        }
    }
}
