package com.elingo.vocabulary;

import com.elingo.BaseIntegrationTest;
import com.elingo.auth.service.JwtService;
import com.elingo.learning.flashcard.entity.UserCardState;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.dto.request.UserDeckPublicationRequest;
import com.elingo.vocabulary.dto.request.UserDeckRequest;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.entity.CefrLevel;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.entity.Tag;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.CefrLevelRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TagRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private TopicRepository topicRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserCardStateRepository userCardStateRepository;

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
                .username("deck_tester")
                .email("deck_tester@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Deck Tester")
                .isActive(true)
                .isVerified(true)
                .role(Role.USER)
                .build());
        authToken = jwtService.generateAccessToken(testUser.getId().toString(), testUser.getRole().name());
    }

    // ─────────────────────── helpers ───────────────────────

    private Tag buildTag(String label) {
        return tagRepository.save(
                Tag.builder().label(label).build()
        );
    }

    private CefrLevel buildCefrLevel(String label) {
        return cefrLevelRepository.save(
                CefrLevel.builder().label(label).build()
        );
    }

    private Deck persistPublishedDeck(String title, String slug,
                                      Set<Tag> tags, Set<CefrLevel> cefrLevels) {
        return deckRepository.save(Deck.builder()
                .title(title)
                .slug(slug)
                .status(DeckStatus.PUBLISHED)
                .ownerType(OwnerType.SYSTEM)
                .publishedAt(LocalDateTime.now())
                .tags(tags)
                .cefrLevels(cefrLevels)
                .build());
    }

    private Deck persistDraftDeck() {
        return deckRepository.save(Deck.builder()
                .title("Hidden Draft")
                .slug("hidden-draft")
                .status(DeckStatus.DRAFT)
                .ownerType(OwnerType.SYSTEM)
                .build());
    }

    private Deck persistUserDeck(User owner, String title, String slug, DeckStatus status, int cardCount) {
        return deckRepository.save(Deck.builder()
                .title(title)
                .slug(slug)
                .status(status)
                .ownerType(OwnerType.USER)
                .owner(owner)
                .cardCount(cardCount)
                .publishedAt(status == DeckStatus.PUBLISHED ? LocalDateTime.now() : null)
                .build());
    }

    private ResultActions performGetDecks(String queryString) throws Exception {
        return mockMvc.perform(get("/decks" + queryString)
                .header("Authorization", "Bearer " + authToken));
    }

    private ResultActions performGetTopicsByDeckId(Long deckId) throws Exception {
        return mockMvc.perform(get("/decks/{deckId}/topics", deckId)
                .header("Authorization", "Bearer " + authToken));
    }

    private ResultActions performGetTopicsByDeckSlug(String slug) throws Exception {
        return mockMvc.perform(get("/decks/slug/{slug}/topics", slug)
                .header("Authorization", "Bearer " + authToken));
    }

    private ResultActions performPostDeck(Object body) throws Exception {
        return mockMvc.perform(post("/decks")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performPutDeck(Long deckId, Object body) throws Exception {
        return mockMvc.perform(put("/decks/{deckId}", deckId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performDeleteDeck(Long deckId) throws Exception {
        return mockMvc.perform(delete("/decks/{deckId}", deckId)
                .header("Authorization", "Bearer " + authToken));
    }

    private ResultActions performPutDeckPublication(Long deckId, Object body) throws Exception {
        return mockMvc.perform(put("/decks/{deckId}/publication", deckId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
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
                    .andExpect(jsonPath("$.data.content[0].learnedCardCount").value(0))
                    .andExpect(jsonPath("$.data.content[1].learnedCardCount").value(0))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        @DisplayName("Get published decks: Success - returns learnedCardCount for user")
        void testGetPublishedDecks_ReturnsLearnedCardCount() throws Exception {
            Deck deck = deckRepository.save(Deck.builder()
                    .title("Animals")
                    .slug("animals")
                    .status(DeckStatus.PUBLISHED)
                    .ownerType(OwnerType.SYSTEM)
                    .publishedAt(LocalDateTime.now())
                    .build());

            Topic topic = topicRepository.save(Topic.builder()
                    .name("Mammals")
                    .slug("mammals")
                    .order(1)
                    .deck(deck)
                    .build());

            Card card1 = cardRepository.save(Card.builder()
                    .term("cat")
                    .translation("con meo")
                    .order(1)
                    .deck(deck)
                    .topic(topic)
                    .build());

            userCardStateRepository.save(UserCardState.builder()
                    .user(testUser)
                    .card(card1)
                    .srsLastGrade((short) 2)
                    .srsNextReviewAt(LocalDateTime.now().plusDays(1))
                    .build());

            performGetDecks("")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].learnedCardCount").value(1));
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
        @DisplayName("Get published decks: Success - pagination works correctly")
        void testGetPublishedDecks_Pagination() throws Exception {
            for (int i = 1; i <= 25; i++) {
                persistPublishedDeck("Deck " + i, "deck-" + i, Set.of(), Set.of());
            }

            performGetDecks("?page=0&size=20")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(20))
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(25))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(false));

            performGetDecks("?page=1&size=20")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(5))
                    .andExpect(jsonPath("$.data.page").value(2))
                    .andExpect(jsonPath("$.data.totalElements").value(25))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        // ─────── filter: CEFR level ───────

        @Test
        @DisplayName("Get published decks: Success - filter by CEFR level code")
        void testGetPublishedDecks_FilterByCefrCode_Success() throws Exception {
            CefrLevel b2 = buildCefrLevel("Upper-Intermediate");
            persistPublishedDeck("B2 Vocabulary", "b2-vocab", Set.of(), Set.of(b2));
            persistPublishedDeck("Other Deck", "other", Set.of(), Set.of());

            performGetDecks("?cefrLevelIds=" + b2.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("B2 Vocabulary"));
        }

        @Test
        @DisplayName("Get published decks: Success - filter by CEFR code returns empty when no match")
        void testGetPublishedDecks_FilterByCefrCode_NotFound() throws Exception {
            CefrLevel a1 = buildCefrLevel("Beginner");
            persistPublishedDeck("A1 Vocab", "a1-vocab", Set.of(), Set.of(a1));

            performGetDecks("?cefrLevelIds=999999")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        // ─────── filter: tag ───────

        @Test
        @DisplayName("Get published decks: Success - filter by tag code")
        void testGetPublishedDecks_FilterByTagCode_Success() throws Exception {
            Tag travel = buildTag("Travel");
            persistPublishedDeck("Travel Words", "travel-words", Set.of(travel), Set.of());
            persistPublishedDeck("Other Deck", "other-2", Set.of(), Set.of());

            performGetDecks("?tagIds=" + travel.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Travel Words"));
        }

        @Test
        @DisplayName("Get published decks: Success - filter by tag code returns empty when no match")
        void testGetPublishedDecks_FilterByTagCode_NotFound() throws Exception {
            Tag food = buildTag("Food");
            persistPublishedDeck("Food Deck", "food-deck", Set.of(food), Set.of());

            performGetDecks("?tagIds=999999")
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

            performGetDecks("?q=english")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("Get published decks: Success - filter by keyword is case-insensitive")
        void testGetPublishedDecks_FilterByKeyword_CaseInsensitive() throws Exception {
            persistPublishedDeck("Animals Deck", "animals-deck", Set.of(), Set.of());

            performGetDecks("?q=ANIMALS")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Animals Deck"));
        }

        @Test
        @DisplayName("Get published decks: Success - filter by keyword returns empty when no match")
        void testGetPublishedDecks_FilterByKeyword_NotFound() throws Exception {
            persistPublishedDeck("Animals Deck", "animals-deck-2", Set.of(), Set.of());

            performGetDecks("?q=zzz_no_match")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        // ─────── combined filters ───────

        @Test
        @DisplayName("Get published decks: Success - combined CEFR and keyword filters")
        void testGetPublishedDecks_CombinedFilters_Success() throws Exception {
            CefrLevel b1 = buildCefrLevel("Intermediate");
            Tag science = buildTag("Science");

            persistPublishedDeck("B1 Science", "b1-science", Set.of(science), Set.of(b1));
            persistPublishedDeck("B1 Travel", "b1-travel", Set.of(), Set.of(b1));
            persistPublishedDeck("A1 Science", "a1-science-2", Set.of(science), Set.of());

            performGetDecks("?cefrLevelIds=" + b1.getId() + "&tagIds=" + science.getId() + "&q=science")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("B1 Science"));
        }

        // ─────── premium filter ───────

        @Test
        @DisplayName("Get published decks: Success - filter by premium true")
        void testGetPublishedDecks_FilterByPremium_True() throws Exception {
            deckRepository.save(Deck.builder()
                    .title("Premium Deck")
                    .slug("premium-deck")
                    .status(DeckStatus.PUBLISHED)
                    .ownerType(OwnerType.SYSTEM)
                    .isPremium(true)
                    .publishedAt(LocalDateTime.now())
                    .build());
            persistPublishedDeck("Free Deck", "free-deck", Set.of(), Set.of());

            performGetDecks("?premium=true")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("Premium Deck"))
                    .andExpect(jsonPath("$.data.content[0].isPremium").value(true));
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
        @DisplayName("Get published decks: Fail because of invalid CEFR level ID")
        void testGetPublishedDecks_Fail_InvalidCefrCode() throws Exception {
            performGetDecks("?cefrLevelIds=-1")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Get published decks: Fail because of invalid tag ID")
        void testGetPublishedDecks_Fail_InvalidTagCode() throws Exception {
            performGetDecks("?tagIds=-1")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }
    }

    @Nested
    @DisplayName("getTopicsByDeckId")
    class GetTopicsByDeckIdTests {

        @Test
        @DisplayName("Get topics by deck ID: Success")
        void testGetTopicsByDeckId_Success() throws Exception {
            Deck deck = deckRepository.save(Deck.builder()
                    .title("Animals Deck")
                    .slug("animals-deck")
                    .status(DeckStatus.PUBLISHED)
                    .ownerType(OwnerType.SYSTEM)
                    .publishedAt(LocalDateTime.now())
                    .build());

            topicRepository.save(Topic.builder()
                    .deck(deck)
                    .name("Mammals")
                    .slug("mammals")
                    .order(1)
                    .build());

            topicRepository.save(Topic.builder()
                    .deck(deck)
                    .name("Birds")
                    .slug("birds")
                    .order(2)
                    .build());

            performGetTopicsByDeckId(deck.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("Mammals"))
                    .andExpect(jsonPath("$.data[1].name").value("Birds"))
                    .andExpect(jsonPath("$.data[0].learnedCardCount").value(0))
                    .andExpect(jsonPath("$.data[1].learnedCardCount").value(0));
        }

        @Test
        @DisplayName("Get topics by deck ID: Success - owner can view topics of own draft deck")
        void testGetTopicsByDeckId_Success_OwnerCanViewOwnDraftDeck() throws Exception {
            Deck deck = persistUserDeck(testUser, "My Draft Deck", "my-draft-deck", DeckStatus.DRAFT, 0);

            topicRepository.save(Topic.builder()
                    .deck(deck)
                    .name("User Topic 1")
                    .slug("user-topic-1")
                    .order(1)
                    .build());

            performGetTopicsByDeckId(deck.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("User Topic 1"));
        }

        @Test
        @DisplayName("Get topics by deck ID: Fail because deck not found")
        void testGetTopicsByDeckId_Fail_NotFound() throws Exception {
            performGetTopicsByDeckId(99999L)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get topics by deck ID: Fail because deck is draft of another owner")
        void testGetTopicsByDeckId_Fail_Draft() throws Exception {
            Deck deck = deckRepository.save(Deck.builder()
                    .title("Draft Deck")
                    .slug("draft-deck")
                    .status(DeckStatus.DRAFT)
                    .ownerType(OwnerType.SYSTEM)
                    .build());

            performGetTopicsByDeckId(deck.getId())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get topics by deck ID: Fail because premium deck without subscription")
        void testGetTopicsByDeckId_Fail_PremiumNoSub() throws Exception {
            Deck deck = deckRepository.save(Deck.builder()
                    .title("Premium Deck")
                    .slug("premium-deck")
                    .status(DeckStatus.PUBLISHED)
                    .ownerType(OwnerType.SYSTEM)
                    .isPremium(true)
                    .publishedAt(LocalDateTime.now())
                    .build());

            performGetTopicsByDeckId(deck.getId())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("PREMIUM_REQUIRED"));
        }
    }

    @Nested
    @DisplayName("getTopicsByDeckSlug")
    class GetTopicsByDeckSlugTests {

        @Test
        @DisplayName("Get topics by deck slug: Success")
        void testGetTopicsByDeckSlug_Success() throws Exception {
            Deck deck = deckRepository.save(Deck.builder()
                    .title("Animals Deck")
                    .slug("animals-deck")
                    .status(DeckStatus.PUBLISHED)
                    .ownerType(OwnerType.SYSTEM)
                    .publishedAt(LocalDateTime.now())
                    .build());

            topicRepository.save(Topic.builder()
                    .deck(deck)
                    .name("Mammals")
                    .slug("mammals")
                    .order(1)
                    .build());

            performGetTopicsByDeckSlug("animals-deck")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Mammals"));
        }

        @Test
        @DisplayName("Get topics by deck slug: Success - owner can view topics of own draft deck")
        void testGetTopicsByDeckSlug_Success_OwnerCanViewOwnDraftDeck() throws Exception {
            Deck deck = persistUserDeck(testUser, "My Draft Deck", "my-draft-deck-slug", DeckStatus.DRAFT, 0);

            topicRepository.save(Topic.builder()
                    .deck(deck)
                    .name("Draft Topic")
                    .slug("draft-topic")
                    .order(1)
                    .build());

            performGetTopicsByDeckSlug("my-draft-deck-slug")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Draft Topic"));
        }

        @Test
        @DisplayName("Get topics by deck slug: Fail because deck not found")
        void testGetTopicsByDeckSlug_Fail_NotFound() throws Exception {
            performGetTopicsByDeckSlug("non-existent-slug")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get topics by deck slug: Fail because deck is draft of another owner")
        void testGetTopicsByDeckSlug_Fail_Draft() throws Exception {
            deckRepository.save(Deck.builder()
                    .title("Draft Deck")
                    .slug("system-draft-deck")
                    .status(DeckStatus.DRAFT)
                    .ownerType(OwnerType.SYSTEM)
                    .build());

            performGetTopicsByDeckSlug("system-draft-deck")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get topics by deck slug: Fail because premium deck without subscription")
        void testGetTopicsByDeckSlug_Fail_PremiumNoSub() throws Exception {
            deckRepository.save(Deck.builder()
                    .title("Premium Deck")
                    .slug("slug-premium-deck")
                    .status(DeckStatus.PUBLISHED)
                    .ownerType(OwnerType.SYSTEM)
                    .isPremium(true)
                    .publishedAt(LocalDateTime.now())
                    .build());

            performGetTopicsByDeckSlug("slug-premium-deck")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("PREMIUM_REQUIRED"));
        }
    }

    @Nested
    @DisplayName("createUserDeck")
    class CreateUserDeckTests {

        @Test
        @DisplayName("Create user deck: Success")
        void testCreateUserDeck_Success() throws Exception {
            UserDeckRequest req = new UserDeckRequest("My Japanese Deck", "Useful Japanese phrases");

            performPostDeck(req)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("My Japanese Deck"))
                    .andExpect(jsonPath("$.data.description").value("Useful Japanese phrases"))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"))
                    .andExpect(jsonPath("$.data.ownerType").value("USER"));

            Deck created = deckRepository.findAllByOwnerTypeAndOwnerId(OwnerType.USER, testUser.getId())
                    .getFirst();
            assertThat(created.getTitle()).isEqualTo("My Japanese Deck");
            assertThat(created.getSlug()).startsWith("my-japanese-deck-");
            assertThat(created.getStatus()).isEqualTo(DeckStatus.DRAFT);
        }

        @Test
        @DisplayName("Create user deck: Fail because of blank title")
        void testCreateUserDeck_Fail_BlankTitle() throws Exception {
            UserDeckRequest req = new UserDeckRequest("   ", "Desc");

            performPostDeck(req)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Create user deck: Fail because of title exceeding max length")
        void testCreateUserDeck_Fail_TitleTooLong() throws Exception {
            UserDeckRequest req = new UserDeckRequest("x".repeat(51), null);

            performPostDeck(req)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Create user deck: Fail because deck limit exceeded (max 5)")
        void testCreateUserDeck_Fail_DeckLimitExceeded() throws Exception {
            for (int i = 1; i <= 5; i++) {
                persistUserDeck(testUser, "Deck " + i, "user-deck-limit-" + i, DeckStatus.DRAFT, 0);
            }

            UserDeckRequest req = new UserDeckRequest("Over Limit Deck", null);

            performPostDeck(req)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_LIMIT_EXCEEDED"));
        }

        @Test
        @DisplayName("Create user deck: Fail because of invalid token")
        void testCreateUserDeck_Fail_InvalidToken() throws Exception {
            mockMvc.perform(post("/decks")
                            .header("Authorization", "Bearer invalid.jwt.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UserDeckRequest("Title", null))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("updateUserDeck")
    class UpdateUserDeckTests {

        @Test
        @DisplayName("Update user deck: Success")
        void testUpdateUserDeck_Success() throws Exception {
            Deck deck = persistUserDeck(testUser, "Old Title", "old-title", DeckStatus.DRAFT, 0);

            UserDeckRequest req = new UserDeckRequest("Updated Title", "Updated Description");

            performPutDeck(deck.getId(), req)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Updated Title"))
                    .andExpect(jsonPath("$.data.description").value("Updated Description"));

            entityManager.flush();
            entityManager.clear();
            Deck updated = deckRepository.findById(deck.getId()).orElseThrow();
            assertThat(updated.getTitle()).isEqualTo("Updated Title");
        }

        @Test
        @DisplayName("Update user deck: Fail because deck not found")
        void testUpdateUserDeck_Fail_NotFound() throws Exception {
            UserDeckRequest req = new UserDeckRequest("Title", null);

            performPutDeck(99999L, req)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Update user deck: Fail because not owner")
        void testUpdateUserDeck_Fail_NotOwner() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .username("other_deck_owner")
                    .email("other_owner@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other Owner")
                    .isActive(true)
                    .isVerified(true)
                    .build());
            Deck otherDeck = persistUserDeck(otherUser, "Other Deck", "other-deck", DeckStatus.PUBLISHED, 1);

            UserDeckRequest req = new UserDeckRequest("Hacked Title", null);

            performPutDeck(otherDeck.getId(), req)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Update user deck: Fail because of blank title")
        void testUpdateUserDeck_Fail_BlankTitle() throws Exception {
            Deck deck = persistUserDeck(testUser, "Old Title", "old-title-2", DeckStatus.DRAFT, 0);

            UserDeckRequest req = new UserDeckRequest("", null);

            performPutDeck(deck.getId(), req)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }
    }

    @Nested
    @DisplayName("deleteUserDeck")
    class DeleteUserDeckTests {

        @Test
        @DisplayName("Delete user deck: Success")
        void testDeleteUserDeck_Success() throws Exception {
            Deck deck = persistUserDeck(testUser, "To Delete", "to-delete", DeckStatus.DRAFT, 0);

            performDeleteDeck(deck.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            assertThat(deckRepository.findById(deck.getId())).isEmpty();
        }

        @Test
        @DisplayName("Delete user deck: Fail because deck not found")
        void testDeleteUserDeck_Fail_NotFound() throws Exception {
            performDeleteDeck(99999L)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Delete user deck: Fail because not owner")
        void testDeleteUserDeck_Fail_NotOwner() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .username("other_deck_owner_2")
                    .email("other_owner2@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other Owner 2")
                    .isActive(true)
                    .isVerified(true)
                    .build());
            Deck otherDeck = persistUserDeck(otherUser, "Other Deck", "other-deck-2", DeckStatus.PUBLISHED, 1);

            performDeleteDeck(otherDeck.getId())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }
    }

    @Nested
    @DisplayName("setUserDeckPublication")
    class SetUserDeckPublicationTests {

        @Test
        @DisplayName("Publish user deck: Success when deck has cards")
        void testPublishUserDeck_Success() throws Exception {
            Deck deck = persistUserDeck(testUser, "Publishable Deck", "pub-deck", DeckStatus.DRAFT, 2);

            performPutDeckPublication(deck.getId(), new UserDeckPublicationRequest(true))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                    .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());

            entityManager.flush();
            entityManager.clear();
            Deck reloaded = deckRepository.findById(deck.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(DeckStatus.PUBLISHED);
            assertThat(reloaded.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Publish user deck: Fail when deck is empty (0 cards)")
        void testPublishUserDeck_Fail_EmptyDeck() throws Exception {
            Deck deck = persistUserDeck(testUser, "Empty Deck", "empty-deck", DeckStatus.DRAFT, 0);

            performPutDeckPublication(deck.getId(), new UserDeckPublicationRequest(true))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_EMPTY"));
        }

        @Test
        @DisplayName("Unpublish user deck: Success")
        void testUnpublishUserDeck_Success() throws Exception {
            Deck deck = persistUserDeck(testUser, "Published Deck", "published-deck", DeckStatus.PUBLISHED, 2);

            performPutDeckPublication(deck.getId(), new UserDeckPublicationRequest(false))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"))
                    .andExpect(jsonPath("$.data.publishedAt").doesNotExist());

            entityManager.flush();
            entityManager.clear();
            Deck reloaded = deckRepository.findById(deck.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(DeckStatus.DRAFT);
            assertThat(reloaded.getPublishedAt()).isNull();
        }

        @Test
        @DisplayName("Set publication: Fail because deck not found")
        void testSetPublication_Fail_NotFound() throws Exception {
            performPutDeckPublication(99999L, new UserDeckPublicationRequest(true))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Set publication: Fail because not owner")
        void testSetPublication_Fail_NotOwner() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .username("other_deck_owner_3")
                    .email("other_owner3@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other Owner 3")
                    .isActive(true)
                    .isVerified(true)
                    .build());
            Deck otherDeck = persistUserDeck(otherUser, "Other Deck", "other-deck-3", DeckStatus.PUBLISHED, 1);

            performPutDeckPublication(otherDeck.getId(), new UserDeckPublicationRequest(false))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }
    }
}
