package com.elingo.admin.content;

import com.elingo.BaseIntegrationTest;
import com.elingo.vocabulary.dto.request.ReorderItemRequest;
import com.elingo.vocabulary.dto.request.SystemDeckRequest;
import com.elingo.vocabulary.dto.request.UpdateDeckStatusRequest;
import com.elingo.auth.service.JwtService;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.entity.*;
import com.elingo.vocabulary.repository.*;
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
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AdminDeckIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private CefrLevelRepository cefrLevelRepository;

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
                .username("admin_deck_user")
                .email("admin_deck@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Admin Deck")
                .isActive(true)
                .isVerified(true)
                .role(Role.ADMIN)
                .build());
        adminToken = jwtService.generateAccessToken(adminUser.getId().toString(), adminUser.getRole().name());

        regularUser = userRepository.save(User.builder()
                .username("regular_deck_user")
                .email("regular_deck@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Regular User")
                .isActive(true)
                .isVerified(true)
                .role(Role.USER)
                .build());
        userToken = jwtService.generateAccessToken(regularUser.getId().toString(), regularUser.getRole().name());
    }

    // ─────────────────────── helpers ───────────────────────

    private Deck persistSystemDeck(String title, String slug, DeckStatus status) {
        return deckRepository.save(Deck.builder()
                .title(title)
                .slug(slug)
                .status(status)
                .ownerType(OwnerType.SYSTEM)
                .owner(null)
                .cardCount(0)
                .topicCount(0)
                .publishedAt(status == DeckStatus.PUBLISHED ? LocalDateTime.now() : null)
                .build());
    }

    private Deck persistUserDeck(User owner, String title, String slug, DeckStatus status) {
        return deckRepository.save(Deck.builder()
                .title(title)
                .slug(slug)
                .status(status)
                .ownerType(OwnerType.USER)
                .owner(owner)
                .cardCount(0)
                .topicCount(0)
                .publishedAt(status == DeckStatus.PUBLISHED ? LocalDateTime.now() : null)
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

    private Tag persistTag(String label) {
        return tagRepository.save(Tag.builder().label(label).build());
    }

    private CefrLevel persistCefrLevel(String label) {
        return cefrLevelRepository.save(CefrLevel.builder().label(label).build());
    }

    // ─────────────────────── test suites ───────────────────────

    @Nested
    @DisplayName("searchDecks")
    class SearchDecksTests {

        @Test
        @DisplayName("Search decks: Success as Admin")
        void testSearchDecks_Success_AsAdmin() throws Exception {
            persistSystemDeck("Admin Search Deck 1", "admin-search-1", DeckStatus.PUBLISHED);
            persistUserDeck(regularUser, "User Search Deck 2", "user-search-2", DeckStatus.DRAFT);

            mockMvc.perform(get("/admin/decks")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isNotEmpty());
        }

        @Test
        @DisplayName("Search decks: Success with filters")
        void testSearchDecks_Success_WithFilters() throws Exception {
            Deck d1 = persistSystemDeck("Unique Alpha Title", "unique-alpha-title", DeckStatus.PUBLISHED);
            persistSystemDeck("Other Deck", "other-deck", DeckStatus.PUBLISHED);

            mockMvc.perform(get("/admin/decks")
                            .param("keyword", "Unique Alpha")
                            .param("ownerType", "SYSTEM")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].id").value(d1.getId()));
        }

        @Test
        @DisplayName("Search decks: Fail because unauthenticated")
        void testSearchDecks_Fail_Unauthenticated() throws Exception {
            mockMvc.perform(get("/admin/decks"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Search decks: Fail because role is regular USER")
        void testSearchDecks_Fail_Forbidden_RegularUser() throws Exception {
            mockMvc.perform(get("/admin/decks")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("getDeckById")
    class GetDeckByIdTests {

        @Test
        @DisplayName("Get deck by ID: Success for system deck")
        void testGetDeckById_Success_SystemDeck() throws Exception {
            Deck deck = persistSystemDeck("System Deck", "system-deck-get", DeckStatus.PUBLISHED);

            mockMvc.perform(get("/admin/decks/{deckId}", deck.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(deck.getId()))
                    .andExpect(jsonPath("$.data.title").value("System Deck"));
        }

        @Test
        @DisplayName("Get deck by ID: Success for user deck")
        void testGetDeckById_Success_UserDeck() throws Exception {
            Deck deck = persistUserDeck(regularUser, "User Deck", "user-deck-get", DeckStatus.DRAFT);

            mockMvc.perform(get("/admin/decks/{deckId}", deck.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(deck.getId()))
                    .andExpect(jsonPath("$.data.ownerType").value("USER"));
        }

        @Test
        @DisplayName("Get deck by ID: Fail when deck does not exist")
        void testGetDeckById_Fail_NotFound() throws Exception {
            mockMvc.perform(get("/admin/decks/{deckId}", 99999L)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("createSystemDeck")
    class CreateSystemDeckTests {

        @Test
        @DisplayName("Create system deck: Success")
        void testCreateSystemDeck_Success() throws Exception {
            Tag tag = persistTag("admin_tag");
            CefrLevel cefr = persistCefrLevel("B2");

            SystemDeckRequest request = new SystemDeckRequest(
                    "New Admin Deck",
                    "Description for admin deck",
                    null,
                    false,
                    Set.of(tag.getId()),
                    Set.of(cefr.getId())
            );

            mockMvc.perform(post("/admin/decks")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("New Admin Deck"))
                    .andExpect(jsonPath("$.data.ownerType").value("SYSTEM"))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"));
        }

        @Test
        @DisplayName("Create system deck: Fail when title is blank")
        void testCreateSystemDeck_Fail_BlankTitle() throws Exception {
            SystemDeckRequest request = new SystemDeckRequest(
                    "   ",
                    "Description",
                    null,
                    false,
                    Set.of(),
                    Set.of()
            );

            mockMvc.perform(post("/admin/decks")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }
    }

    @Nested
    @DisplayName("updateSystemDeck")
    class UpdateSystemDeckTests {

        @Test
        @DisplayName("Update system deck: Success")
        void testUpdateSystemDeck_Success() throws Exception {
            Deck deck = persistSystemDeck("Old Admin Deck", "old-admin-deck", DeckStatus.PUBLISHED);

            SystemDeckRequest request = new SystemDeckRequest(
                    "Updated Admin Deck",
                    "Updated description",
                    null,
                    true,
                    Set.of(),
                    Set.of()
            );

            mockMvc.perform(put("/admin/decks/{deckId}", deck.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Updated Admin Deck"))
                    .andExpect(jsonPath("$.data.isPremium").value(true));
        }

        @Test
        @DisplayName("Update system deck: Fail when deck is USER owned")
        void testUpdateSystemDeck_Fail_NotSystemDeck() throws Exception {
            Deck userDeck = persistUserDeck(regularUser, "User Deck", "user-deck-update", DeckStatus.DRAFT);

            SystemDeckRequest request = new SystemDeckRequest(
                    "Updated User Deck",
                    "Updated description",
                    null,
                    false,
                    Set.of(),
                    Set.of()
            );

            mockMvc.perform(put("/admin/decks/{deckId}", userDeck.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_SYSTEM"));
        }

        @Test
        @DisplayName("Update system deck: Fail when deck not found")
        void testUpdateSystemDeck_Fail_NotFound() throws Exception {
            SystemDeckRequest request = new SystemDeckRequest(
                    "Updated Title",
                    "Updated description",
                    null,
                    false,
                    Set.of(),
                    Set.of()
            );

            mockMvc.perform(put("/admin/decks/{deckId}", 99999L)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("updateDeckStatus")
    class UpdateDeckStatusTests {

        @Test
        @DisplayName("Update deck status: Success publish system deck")
        void testUpdateDeckStatus_Success_Publish() throws Exception {
            Deck deck = persistSystemDeck("Draft System Deck", "draft-sys-deck", DeckStatus.DRAFT);
            deck.setCardCount(3);
            deckRepository.save(deck);

            mockMvc.perform(patch("/admin/decks/{deckId}/status", deck.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateDeckStatusRequest(DeckStatus.PUBLISHED))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

            entityManager.flush();
            entityManager.clear();
            Deck updated = deckRepository.findById(deck.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(DeckStatus.PUBLISHED);
            assertThat(updated.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Update deck status: Success archive system deck")
        void testUpdateDeckStatus_Success_Archive() throws Exception {
            Deck deck = persistSystemDeck("Published Deck", "pub-sys-deck", DeckStatus.PUBLISHED);

            mockMvc.perform(patch("/admin/decks/{deckId}/status", deck.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateDeckStatusRequest(DeckStatus.ARCHIVED))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
        }

        @Test
        @DisplayName("Update deck status: Fail when deck is USER owned")
        void testUpdateDeckStatus_Fail_NotSystemDeck() throws Exception {
            Deck userDeck = persistUserDeck(regularUser, "User Deck", "user-deck-status", DeckStatus.DRAFT);

            mockMvc.perform(patch("/admin/decks/{deckId}/status", userDeck.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateDeckStatusRequest(DeckStatus.PUBLISHED))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_SYSTEM"));
        }
    }

    @Nested
    @DisplayName("deleteUserDeck")
    class DeleteUserDeckTests {

        @Test
        @DisplayName("Delete user deck by admin: Success")
        void testDeleteUserDeck_Success() throws Exception {
            Deck userDeck = persistUserDeck(regularUser, "Spam Deck", "spam-deck", DeckStatus.PUBLISHED);

            mockMvc.perform(delete("/admin/decks/{deckId}", userDeck.getId())
                            .param("reason", "Spam content")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            entityManager.flush();
            entityManager.clear();
            assertThat(deckRepository.findById(userDeck.getId())).isEmpty();
        }

        @Test
        @DisplayName("Delete user deck by admin: Fail when deck is SYSTEM owned")
        void testDeleteUserDeck_Fail_NotUserDeck() throws Exception {
            Deck systemDeck = persistSystemDeck("System Deck", "system-deck-delete", DeckStatus.PUBLISHED);

            mockMvc.perform(delete("/admin/decks/{deckId}", systemDeck.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("CANNOT_DELETE_SYSTEM_DECK"));
        }

        @Test
        @DisplayName("Delete user deck by admin: Fail when deck not found")
        void testDeleteUserDeck_Fail_NotFound() throws Exception {
            mockMvc.perform(delete("/admin/decks/{deckId}", 99999L)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("getTopicsByDeckId")
    class GetTopicsByDeckIdTests {

        @Test
        @DisplayName("Get topics by deck ID: Success as Admin")
        void testGetTopicsByDeckId_Success() throws Exception {
            Deck systemDeck = persistSystemDeck("System Deck", "system-deck-topics", DeckStatus.PUBLISHED);
            persistTopic(systemDeck, "Topic A", "topic-a", 1);
            persistTopic(systemDeck, "Topic B", "topic-b", 2);

            mockMvc.perform(get("/admin/decks/{deckId}/topics", systemDeck.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @DisplayName("Get topics by deck ID: Fail when deck not found")
        void testGetTopicsByDeckId_Fail_NotFound() throws Exception {
            mockMvc.perform(get("/admin/decks/{deckId}/topics", 99999L)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("reorderSystemTopics")
    class ReorderSystemTopicsTests {

        @Test
        @DisplayName("Reorder system topics: Success")
        void testReorderSystemTopics_Success() throws Exception {
            Deck systemDeck = persistSystemDeck("System Deck", "system-deck-reorder", DeckStatus.PUBLISHED);
            Topic topic1 = persistTopic(systemDeck, "Topic 1", "topic-1", 1);
            Topic topic2 = persistTopic(systemDeck, "Topic 2", "topic-2", 2);

            ReorderItemRequest request = new ReorderItemRequest(List.of(topic2.getId(), topic1.getId()));

            mockMvc.perform(patch("/admin/decks/{deckId}/topics/orders", systemDeck.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            entityManager.flush();
            entityManager.clear();
            Topic reloaded1 = topicRepository.findById(topic1.getId()).orElseThrow();
            Topic reloaded2 = topicRepository.findById(topic2.getId()).orElseThrow();
            assertThat(reloaded2.getOrder()).isEqualTo(1);
            assertThat(reloaded1.getOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("Reorder system topics: Fail when deck is USER owned")
        void testReorderSystemTopics_Fail_NotSystemDeck() throws Exception {
            Deck userDeck = persistUserDeck(regularUser, "User Deck", "user-deck-reorder", DeckStatus.DRAFT);
            Topic topic1 = persistTopic(userDeck, "Topic 1", "u-topic-1", 1);

            ReorderItemRequest request = new ReorderItemRequest(List.of(topic1.getId()));

            mockMvc.perform(patch("/admin/decks/{deckId}/topics/orders", userDeck.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_SYSTEM"));
        }

        @Test
        @DisplayName("Reorder system topics: Fail when topic does not belong to deck")
        void testReorderSystemTopics_Fail_TopicMismatch() throws Exception {
            Deck deck1 = persistSystemDeck("System Deck 1", "system-deck-1", DeckStatus.PUBLISHED);
            Deck deck2 = persistSystemDeck("System Deck 2", "system-deck-2", DeckStatus.PUBLISHED);
            Topic topicInDeck2 = persistTopic(deck2, "Topic 2", "topic-2", 1);

            ReorderItemRequest request = new ReorderItemRequest(List.of(topicInDeck2.getId()));

            mockMvc.perform(patch("/admin/decks/{deckId}/topics/orders", deck1.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }
    }
}
