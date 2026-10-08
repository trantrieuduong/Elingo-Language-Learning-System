package com.elingo.vocabulary;

import com.elingo.BaseIntegrationTest;
import com.elingo.auth.service.JwtService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.entity.CefrLevel;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.entity.Tag;
import com.elingo.vocabulary.repository.CefrLevelRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TagRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DeckIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private CefrLevelRepository cefrLevelRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;

    @BeforeEach
    void setUp() {
        User testUser = userRepository.save(User.builder()
                .username("deck_tester")
                .email("deck_tester@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Deck Tester")
                .isActive(true)
                .isVerified(true)
                .build());

        String authorities = testUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        authToken = jwtService.generateAccessToken(testUser.getId().toString(), authorities);
    }

    // ─────────────────────── helpers ───────────────────────

    private Tag buildTag(String code, String label) {
        return tagRepository.save(
                Tag.builder().code(code).label(label).build()
        );
    }

    private CefrLevel buildCefrLevel(String code, String label) {
        return cefrLevelRepository.save(
                CefrLevel.builder().code(code).label(label).build()
        );
    }

    private void persistPublishedDeck(String title, String slug,
                                      Set<Tag> tags, Set<CefrLevel> cefrLevels) {
        deckRepository.save(Deck.builder()
                .title(title)
                .slug(slug)
                .status(DeckStatus.PUBLISHED)
                .ownerType(OwnerType.SYSTEM)
                .publishedAt(LocalDateTime.now())
                .tags(tags)
                .cefrLevels(cefrLevels)
                .build());
    }

    private void persistDraftDeck() {
        deckRepository.save(Deck.builder()
                .title("Hidden Draft")
                .slug("hidden-draft")
                .status(DeckStatus.DRAFT)
                .ownerType(OwnerType.SYSTEM)
                .build());
    }

    private ResultActions performGetDecks(String queryString) throws Exception {
        return mockMvc.perform(get("/decks" + queryString)
                .header("Authorization", "Bearer " + authToken));
    }

    // ─────────────────────── test class ───────────────────────

    @Nested
    @DisplayName("getPublishedDecks")
    class GetPublishedDecksTests {

        @Test
        @DisplayName("Get published decks: Success - returns paginated published decks")
        void testGetPublishedDecks_Success() throws Exception {
            persistPublishedDeck("Animals", "animals", Set.of(), Set.of());
            persistPublishedDeck("Colors", "colors", Set.of(), Set.of());

            performGetDecks("")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.size").value(9))
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        @DisplayName("Get published decks: Success - excludes draft decks")
        void testGetPublishedDecks_ExcludesDraftDecks() throws Exception {
            persistPublishedDeck("Animals", "animals", Set.of(), Set.of());
            persistDraftDeck();

            performGetDecks("")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Animals"));
        }

        @Test
        @DisplayName("Get published decks: Success - returns empty when no published decks")
        void testGetPublishedDecks_ReturnsEmpty_WhenNoDeck() throws Exception {
            performGetDecks("")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("Get published decks: Success - second page returns correct slice")
        void testGetPublishedDecks_Pagination_SecondPage() throws Exception {
            // Persist 10 decks so page 1 has 1 deck
            for (int i = 1; i <= 10; i++) {
                persistPublishedDeck("Deck " + i, "deck-" + i, Set.of(), Set.of());
            }

            performGetDecks("?page=2")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.page").value(2))
                    .andExpect(jsonPath("$.data.totalElements").value(10))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        // ─────── filter: CEFR level ───────

        @Test
        @DisplayName("Get published decks: Success - filter by CEFR level code")
        void testGetPublishedDecks_FilterByCefrCode_Success() throws Exception {
            CefrLevel b2 = buildCefrLevel("B2", "Upper-Intermediate");
            persistPublishedDeck("B2 Vocabulary", "b2-vocab", Set.of(), Set.of(b2));
            persistPublishedDeck("Other Deck", "other", Set.of(), Set.of());

            performGetDecks("?cefrCode=B2")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("B2 Vocabulary"));
        }

        @Test
        @DisplayName("Get published decks: Success - filter by CEFR code returns empty when no match")
        void testGetPublishedDecks_FilterByCefrCode_NotFound() throws Exception {
            CefrLevel a1 = buildCefrLevel("A1", "Beginner");
            persistPublishedDeck("A1 Vocab", "a1-vocab", Set.of(), Set.of(a1));

            performGetDecks("?cefrCode=C2")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        // ─────── filter: tag ───────

        @Test
        @DisplayName("Get published decks: Success - filter by tag code")
        void testGetPublishedDecks_FilterByTagCode_Success() throws Exception {
            Tag travel = buildTag("TRAVEL", "Travel");
            persistPublishedDeck("Travel Words", "travel-words", Set.of(travel), Set.of());
            persistPublishedDeck("Other Deck", "other-2", Set.of(), Set.of());

            performGetDecks("?tagCode=TRAVEL")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Travel Words"));
        }

        @Test
        @DisplayName("Get published decks: Success - filter by tag code returns empty when no match")
        void testGetPublishedDecks_FilterByTagCode_NotFound() throws Exception {
            Tag food = buildTag("FOOD", "Food");
            persistPublishedDeck("Food Deck", "food-deck", Set.of(food), Set.of());

            performGetDecks("?tagCode=TRAVEL")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        // ─────── filter: keyword ───────

        @Test
        @DisplayName("Get published decks: Success - filter by keyword in title")
        void testGetPublishedDecks_FilterByKeyword_Success() throws Exception {
            persistPublishedDeck("English Animals", "english-animals", Set.of(), Set.of());
            persistPublishedDeck("English Colors", "english-colors", Set.of(), Set.of());
            persistPublishedDeck("French Basic", "french-basic", Set.of(), Set.of());

            performGetDecks("?keyword=english")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("Get published decks: Success - filter by keyword is case-insensitive")
        void testGetPublishedDecks_FilterByKeyword_CaseInsensitive() throws Exception {
            persistPublishedDeck("Animals Deck", "animals-deck", Set.of(), Set.of());

            performGetDecks("?keyword=ANIMALS")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Animals Deck"));
        }

        @Test
        @DisplayName("Get published decks: Success - filter by keyword returns empty when no match")
        void testGetPublishedDecks_FilterByKeyword_NotFound() throws Exception {
            persistPublishedDeck("Animals Deck", "animals-deck-2", Set.of(), Set.of());

            performGetDecks("?keyword=zzz_no_match")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        // ─────── combined filters ───────

        @Test
        @DisplayName("Get published decks: Success - combined CEFR and keyword filters")
        void testGetPublishedDecks_CombinedFilters_Success() throws Exception {
            CefrLevel b1 = buildCefrLevel("B1", "Intermediate");
            Tag science = buildTag("SCIENCE", "Science");

            persistPublishedDeck("B1 Science", "b1-science", Set.of(science), Set.of(b1));
            persistPublishedDeck("B1 Travel", "b1-travel", Set.of(), Set.of(b1));
            persistPublishedDeck("A1 Science", "a1-science-2", Set.of(science), Set.of());

            performGetDecks("?cefrCode=B1&keyword=science")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("B1 Science"));
        }

        // ─────── security ───────

        @Test
        @DisplayName("Get published decks: Fail because of invalid token")
        void testGetPublishedDecks_Fail_InvalidToken() throws Exception {
            mockMvc.perform(get("/decks")
                            .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }

        // ─────── input validation ───────

        @Test
        @DisplayName("Get published decks: Fail because of invalid CEFR code")
        void testGetPublishedDecks_Fail_InvalidCefrCode() throws Exception {
            performGetDecks("?cefrCode=B9999999999")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("CEFR_LEVEL_CODE_INVALID"));
        }

        @Test
        @DisplayName("Get published decks: Fail because of negative page index")
        void testGetPublishedDecks_Fail_NegativePage() throws Exception {
            performGetDecks("?page=-1")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
