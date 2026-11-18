package com.elingo.admin.content;

import com.elingo.BaseIntegrationTest;
import com.elingo.auth.service.JwtService;
import com.elingo.user.entity.Role;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminOrphanedCardIntegrationTest extends BaseIntegrationTest {

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

    private User adminUser;
    private String adminToken;

    private User regularUser;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .username("admin_orphan_user")
                .email("admin_orphan@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Admin Orphan")
                .isActive(true)
                .isVerified(true)
                .role(Role.ADMIN)
                .build());
        adminToken = jwtService.generateAccessToken(adminUser.getId().toString(), adminUser.getRole().name());

        regularUser = userRepository.save(User.builder()
                .username("regular_orphan_user")
                .email("regular_orphan@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Regular User")
                .isActive(true)
                .isVerified(true)
                .role(Role.USER)
                .build());
        userToken = jwtService.generateAccessToken(regularUser.getId().toString(), regularUser.getRole().name());
    }

    // ─────────────────────── helpers ───────────────────────

    private Card persistOrphanCard(String term) {
        return cardRepository.save(Card.builder()
                .term(term)
                .translation("nghĩa")
                .deck(null)
                .topic(null)
                .order(0)
                .build());
    }

    private Card persistNormalCard(String term) {
        Deck deck = deckRepository.save(Deck.builder()
                .title("System Deck")
                .slug("sys-deck-" + System.nanoTime())
                .status(DeckStatus.PUBLISHED)
                .ownerType(OwnerType.SYSTEM)
                .cardCount(1)
                .topicCount(1)
                .publishedAt(LocalDateTime.now())
                .build());
        Topic topic = topicRepository.save(Topic.builder()
                .deck(deck)
                .name("Topic")
                .slug("topic-" + System.nanoTime())
                .order(1)
                .cardCount(1)
                .build());
        return cardRepository.save(Card.builder()
                .deck(deck)
                .topic(topic)
                .term(term)
                .translation("nghĩa")
                .order(1)
                .build());
    }

    // ─────────────────────── test suites ───────────────────────

    @Nested
    @DisplayName("getStats")
    class GetStatsTests {

        @Test
        @DisplayName("Get orphaned card stats: Success")
        void testGetStats_Success() throws Exception {
            persistOrphanCard("orphan1");
            persistOrphanCard("orphan2");
            persistNormalCard("normal1");

            mockMvc.perform(get("/admin/orphaned-cards/stats")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalOrphanedCards").isNumber());
        }

        @Test
        @DisplayName("Get orphaned card stats: Fail when role is USER")
        void testGetStats_Fail_Forbidden_UserRole() throws Exception {
            mockMvc.perform(get("/admin/orphaned-cards/stats")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Get orphaned card stats: Fail when unauthenticated")
        void testGetStats_Fail_Unauthenticated() throws Exception {
            mockMvc.perform(get("/admin/orphaned-cards/stats"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("getOrphanedCards")
    class GetOrphanedCardsTests {

        @Test
        @DisplayName("Get orphaned cards list: Success")
        void testGetOrphanedCards_Success() throws Exception {
            persistOrphanCard("orphan_card_1");

            mockMvc.perform(get("/admin/orphaned-cards")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(1)));
        }

        @Test
        @DisplayName("Get orphaned cards list: Fail when role is USER")
        void testGetOrphanedCards_Fail_Forbidden_UserRole() throws Exception {
            mockMvc.perform(get("/admin/orphaned-cards")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("cleanupOrphanedCards")
    class CleanupOrphanedCardsTests {

        @Test
        @DisplayName("Cleanup orphaned cards: Success")
        void testCleanupOrphanedCards_Success() throws Exception {
            Card orphan = persistOrphanCard("orphan_to_clean");
            Card normal = persistNormalCard("normal_stay");

            mockMvc.perform(post("/admin/orphaned-cards/cleanup")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.deletedCardCount").value(1));

            entityManager.flush();
            entityManager.clear();
            assertThat(cardRepository.findById(orphan.getId())).isEmpty();
            assertThat(cardRepository.findById(normal.getId())).isPresent();
        }

        @Test
        @DisplayName("Cleanup orphaned cards: Fail when role is USER")
        void testCleanupOrphanedCards_Fail_Forbidden_UserRole() throws Exception {
            mockMvc.perform(post("/admin/orphaned-cards/cleanup")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }
    }
}
