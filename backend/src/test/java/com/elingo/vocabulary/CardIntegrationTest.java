package com.elingo.vocabulary;

import com.elingo.BaseIntegrationTest;
import com.elingo.auth.service.JwtService;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.dto.request.CreateUserCardRequest;
import com.elingo.vocabulary.dto.request.UpdateUserCardRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CardIntegrationTest extends BaseIntegrationTest {

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

    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .username("card_tester")
                .email("card_tester@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Card Tester")
                .isActive(true)
                .isVerified(true)
                .role(Role.USER)
                .build());

        authToken = jwtService.generateAccessToken(testUser.getId().toString(), testUser.getRole().name());
    }

    // ─────────────────────── helpers ───────────────────────

    private Deck persistUserDeck(User owner, String title, String slug, DeckStatus status) {
        return deckRepository.save(Deck.builder()
                .title(title)
                .slug(slug)
                .status(status)
                .ownerType(OwnerType.USER)
                .owner(owner)
                .cardCount(0)
                .topicCount(0)
                .build());
    }

    private Topic persistTopic(Deck deck, String name, String slug, int order) {
        Topic topic = topicRepository.save(Topic.builder()
                .deck(deck)
                .name(name)
                .slug(slug)
                .order(order)
                .cardCount(0)
                .build());
        deckRepository.adjustTopicCount(deck.getId(), 1);
        return topic;
    }

    private Card persistCard(Deck deck, Topic topic, String term, String translation, int order) {
        Card card = cardRepository.save(Card.builder()
                .deck(deck)
                .topic(topic)
                .term(term)
                .translation(translation)
                .order(order)
                .build());
        topicRepository.adjustCardCount(topic.getId(), 1);
        deckRepository.adjustCardCount(deck.getId(), 1);
        return card;
    }

    private ResultActions performPostCard(Object body) throws Exception {
        return mockMvc.perform(post("/cards")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performPutCard(Long cardId, Object body) throws Exception {
        return mockMvc.perform(put("/cards/{cardId}", cardId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performDeleteCard(Long cardId) throws Exception {
        return mockMvc.perform(delete("/cards/{cardId}", cardId)
                .header("Authorization", "Bearer " + authToken));
    }

    // ─────────────────────── createUserCard ───────────────────────

    @Nested
    @DisplayName("createUserCard")
    class CreateUserCardTests {

        @Test
        @DisplayName("Create user card: Success - creates card, sets order to 1 and increments cardCounts")
        void testCreateUserCard_Success() throws Exception {
            Deck deck = persistUserDeck(testUser, "Vocabulary Deck", "vocab-deck", DeckStatus.DRAFT);
            Topic topic = persistTopic(deck, "Animals", "animals", 1);

            CreateUserCardRequest request = new CreateUserCardRequest(
                    topic.getId(),
                    "cat",
                    "n",
                    "con meo",
                    "dong vat bon chan",
                    "a four-legged animal",
                    "Meo bat chuot",
                    "The cat catches mice"
            );

            performPostCard(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.term").value("cat"))
                    .andExpect(jsonPath("$.data.pos").value("n"))
                    .andExpect(jsonPath("$.data.translation").value("con meo"))
                    .andExpect(jsonPath("$.data.order").value(1));

            entityManager.flush();
            entityManager.clear();

            Card saved = cardRepository.findByTopicIdWithPhonetics(topic.getId()).getFirst();
            assertThat(saved.getTerm()).isEqualTo("cat");
            assertThat(saved.getOrder()).isEqualTo(1);
            assertThat(saved.getDeck().getId()).isEqualTo(deck.getId());
            assertThat(saved.getTopic().getId()).isEqualTo(topic.getId());

            Topic reloadedTopic = topicRepository.findById(topic.getId()).orElseThrow();
            assertThat(reloadedTopic.getCardCount()).isEqualTo(1);

            Deck reloadedDeck = deckRepository.findById(deck.getId()).orElseThrow();
            assertThat(reloadedDeck.getCardCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Create user card: Success - auto increments order for subsequent cards")
        void testCreateUserCard_Success_AutoIncrementsOrder() throws Exception {
            Deck deck = persistUserDeck(testUser, "Vocabulary Deck", "vocab-deck-order", DeckStatus.DRAFT);
            Topic topic = persistTopic(deck, "Animals", "animals-order", 1);

            performPostCard(new CreateUserCardRequest(topic.getId(), "cat", "n", "con meo", null, null, null, null))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.order").value(1));

            performPostCard(new CreateUserCardRequest(topic.getId(), "dog", "n", "con cho", null, null, null, null))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.order").value(2));

            entityManager.flush();
            entityManager.clear();

            Topic reloadedTopic = topicRepository.findById(topic.getId()).orElseThrow();
            assertThat(reloadedTopic.getCardCount()).isEqualTo(2);

            Deck reloadedDeck = deckRepository.findById(deck.getId()).orElseThrow();
            assertThat(reloadedDeck.getCardCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Create user card: Success - trims input and normalizes pos to lowercase")
        void testCreateUserCard_Success_NormalizesInput() throws Exception {
            Deck deck = persistUserDeck(testUser, "Vocabulary Deck", "vocab-deck-trim", DeckStatus.DRAFT);
            Topic topic = persistTopic(deck, "Animals", "animals-trim", 1);

            CreateUserCardRequest request = new CreateUserCardRequest(
                    topic.getId(),
                    "  elephant  ",
                    "  NOUN  ",
                    "  con voi  ",
                    "   ",
                    "   ",
                    "   ",
                    "   "
            );

            performPostCard(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.term").value("elephant"))
                    .andExpect(jsonPath("$.data.pos").value("noun"))
                    .andExpect(jsonPath("$.data.translation").value("con voi"));

            entityManager.flush();
            entityManager.clear();

            Card saved = cardRepository.findByTopicIdWithPhonetics(topic.getId()).getFirst();
            assertThat(saved.getTerm()).isEqualTo("elephant");
            assertThat(saved.getPos()).isEqualTo("noun");
            assertThat(saved.getTranslation()).isEqualTo("con voi");
            assertThat(saved.getExplanationVi()).isNull();
            assertThat(saved.getExplanationEn()).isNull();
            assertThat(saved.getExamplesVi()).isNull();
            assertThat(saved.getExamplesEn()).isNull();
        }

        @Test
        @DisplayName("Create user card: Fail because topic not found")
        void testCreateUserCard_Fail_TopicNotFound() throws Exception {
            CreateUserCardRequest request = new CreateUserCardRequest(
                    99999L, "cat", "n", "con meo", null, null, null, null);

            performPostCard(request)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }

        @Test
        @DisplayName("Create user card: Fail because user does not own the deck")
        void testCreateUserCard_Fail_NotOwner() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .username("other_card_owner")
                    .email("other_card@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other Card Owner")
                    .isActive(true)
                    .isVerified(true)
                    .role(Role.USER)
                    .build());
            Deck otherDeck = persistUserDeck(otherUser, "Other Deck", "other-card-deck", DeckStatus.PUBLISHED);
            Topic otherTopic = persistTopic(otherDeck, "Other Topic", "other-topic", 1);

            CreateUserCardRequest request = new CreateUserCardRequest(
                    otherTopic.getId(), "hacked", "n", "hack", null, null, null, null);

            performPostCard(request)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Create user card: Fail because deck is draft of another user")
        void testCreateUserCard_Fail_DraftDeckNotOwner() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .username("other_card_owner_draft")
                    .email("other_card_draft@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other Card Owner Draft")
                    .isActive(true)
                    .isVerified(true)
                    .role(Role.USER)
                    .build());
            Deck otherDeck = persistUserDeck(otherUser, "Other Draft Deck", "other-card-deck-draft", DeckStatus.DRAFT);
            Topic otherTopic = persistTopic(otherDeck, "Other Draft Topic", "other-topic-draft", 1);

            CreateUserCardRequest request = new CreateUserCardRequest(
                    otherTopic.getId(), "secret", "n", "bi mat", null, null, null, null);

            performPostCard(request)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Create user card: Fail because card limit exceeded (max 50 cards per topic)")
        void testCreateUserCard_Fail_CardLimitExceeded() throws Exception {
            Deck deck = persistUserDeck(testUser, "Large Deck", "large-deck", DeckStatus.DRAFT);
            Topic topic = persistTopic(deck, "Large Topic", "large-topic", 1);

            for (int i = 1; i <= 50; i++) {
                cardRepository.save(Card.builder()
                        .deck(deck)
                        .topic(topic)
                        .term("word" + i)
                        .translation("nghia" + i)
                        .order(i)
                        .build());
            }

            CreateUserCardRequest request = new CreateUserCardRequest(
                    topic.getId(), "overflow", "n", "qua tai", null, null, null, null);

            performPostCard(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("CARD_LIMIT_EXCEEDED"));
        }

        @Test
        @DisplayName("Create user card: Fail because term is blank")
        void testCreateUserCard_Fail_BlankTerm() throws Exception {
            Deck deck = persistUserDeck(testUser, "Deck", "deck-blank-term", DeckStatus.DRAFT);
            Topic topic = persistTopic(deck, "Topic", "topic-blank-term", 1);

            CreateUserCardRequest request = new CreateUserCardRequest(
                    topic.getId(), "   ", "n", "con meo", null, null, null, null);

            performPostCard(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Create user card: Fail because translation is blank")
        void testCreateUserCard_Fail_BlankTranslation() throws Exception {
            Deck deck = persistUserDeck(testUser, "Deck", "deck-blank-trans", DeckStatus.DRAFT);
            Topic topic = persistTopic(deck, "Topic", "topic-blank-trans", 1);

            CreateUserCardRequest request = new CreateUserCardRequest(
                    topic.getId(), "cat", "n", "   ", null, null, null, null);

            performPostCard(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Create user card: Fail because term exceeds max length 100")
        void testCreateUserCard_Fail_TermTooLong() throws Exception {
            Deck deck = persistUserDeck(testUser, "Deck", "deck-term-long", DeckStatus.DRAFT);
            Topic topic = persistTopic(deck, "Topic", "topic-term-long", 1);

            CreateUserCardRequest request = new CreateUserCardRequest(
                    topic.getId(), "x".repeat(101), "n", "con meo", null, null, null, null);

            performPostCard(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Create user card: Fail because translation exceeds max length 500")
        void testCreateUserCard_Fail_TranslationTooLong() throws Exception {
            Deck deck = persistUserDeck(testUser, "Deck", "deck-trans-long", DeckStatus.DRAFT);
            Topic topic = persistTopic(deck, "Topic", "topic-trans-long", 1);

            CreateUserCardRequest request = new CreateUserCardRequest(
                    topic.getId(), "cat", "n", "x".repeat(501), null, null, null, null);

            performPostCard(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Create user card: Fail because missing token")
        void testCreateUserCard_Fail_Unauthenticated() throws Exception {
            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateUserCardRequest(
                                    1L, "cat", "n", "con meo", null, null, null, null))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Create user card: Fail because invalid token")
        void testCreateUserCard_Fail_InvalidToken() throws Exception {
            mockMvc.perform(post("/cards")
                            .header("Authorization", "Bearer invalid.jwt.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateUserCardRequest(
                                    1L, "cat", "n", "con meo", null, null, null, null))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────── updateUserCard ───────────────────────

    @Nested
    @DisplayName("updateUserCard")
    class UpdateUserCardTests {

        @Test
        @DisplayName("Update user card: Success")
        void testUpdateUserCard_Success() throws Exception {
            Deck deck = persistUserDeck(testUser, "Deck", "deck-update-card", DeckStatus.DRAFT);
            Topic topic = persistTopic(deck, "Topic", "topic-update-card", 1);
            Card card = persistCard(deck, topic, "cat", "con meo", 1);

            UpdateUserCardRequest request = new UpdateUserCardRequest(
                    "dog",
                    "n",
                    "con cho",
                    "loai dong vat trung thanh",
                    "a loyal animal",
                    "Cho coi nha",
                    "Dogs guard the house"
            );

            performPutCard(card.getId(), request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.term").value("dog"))
                    .andExpect(jsonPath("$.data.translation").value("con cho"));

            entityManager.flush();
            entityManager.clear();

            Card reloaded = cardRepository.findById(card.getId()).orElseThrow();
            assertThat(reloaded.getTerm()).isEqualTo("dog");
            assertThat(reloaded.getTranslation()).isEqualTo("con cho");
            assertThat(reloaded.getExplanationVi()).isEqualTo("loai dong vat trung thanh");
            assertThat(reloaded.getExplanationEn()).isEqualTo("a loyal animal");
        }

        @Test
        @DisplayName("Update user card: Fail because card not found")
        void testUpdateUserCard_Fail_CardNotFound() throws Exception {
            UpdateUserCardRequest request = new UpdateUserCardRequest(
                    "dog", "n", "con cho", null, null, null, null);

            performPutCard(99999L, request)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("CARD_NOT_FOUND"));
        }

        @Test
        @DisplayName("Update user card: Fail because card belongs to another user")
        void testUpdateUserCard_Fail_NotOwner() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .username("other_card_editor")
                    .email("other_editor@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other Card Editor")
                    .isActive(true)
                    .isVerified(true)
                    .role(Role.USER)
                    .build());
            Deck otherDeck = persistUserDeck(otherUser, "Other Deck", "other-edit-deck", DeckStatus.PUBLISHED);
            Topic otherTopic = persistTopic(otherDeck, "Other Topic", "other-edit-topic", 1);
            Card otherCard = persistCard(otherDeck, otherTopic, "cat", "con meo", 1);

            UpdateUserCardRequest request = new UpdateUserCardRequest(
                    "hacked", "n", "hack", null, null, null, null);

            performPutCard(otherCard.getId(), request)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Update user card: Fail because card is in draft deck of another user")
        void testUpdateUserCard_Fail_DraftDeckNotOwner() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .username("other_card_draft_editor")
                    .email("other_draft_editor@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other Card Draft Editor")
                    .isActive(true)
                    .isVerified(true)
                    .role(Role.USER)
                    .build());
            Deck otherDeck = persistUserDeck(otherUser, "Other Draft Deck", "other-edit-draft-deck", DeckStatus.DRAFT);
            Topic otherTopic = persistTopic(otherDeck, "Other Topic", "other-edit-draft-topic", 1);
            Card otherCard = persistCard(otherDeck, otherTopic, "secret", "bi mat", 1);

            UpdateUserCardRequest request = new UpdateUserCardRequest(
                    "leak", "n", "lo", null, null, null, null);

            performPutCard(otherCard.getId(), request)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Update user card: Fail because term is blank")
        void testUpdateUserCard_Fail_BlankTerm() throws Exception {
            Deck deck = persistUserDeck(testUser, "Deck", "deck-update-blank", DeckStatus.DRAFT);
            Topic topic = persistTopic(deck, "Topic", "topic-update-blank", 1);
            Card card = persistCard(deck, topic, "cat", "con meo", 1);

            UpdateUserCardRequest request = new UpdateUserCardRequest(
                    "   ", "n", "con meo", null, null, null, null);

            performPutCard(card.getId(), request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Update user card: Fail because missing token")
        void testUpdateUserCard_Fail_Unauthenticated() throws Exception {
            mockMvc.perform(put("/cards/{cardId}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserCardRequest(
                                    "cat", "n", "con meo", null, null, null, null))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Update user card: Fail because invalid token")
        void testUpdateUserCard_Fail_InvalidToken() throws Exception {
            mockMvc.perform(put("/cards/{cardId}", 1L)
                            .header("Authorization", "Bearer invalid.jwt.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserCardRequest(
                                    "cat", "n", "con meo", null, null, null, null))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────── deleteUserCard ───────────────────────

    @Nested
    @DisplayName("deleteUserCard")
    class DeleteUserCardTests {

        @Test
        @DisplayName("Delete user card: Success - deletes card and decrements topic and deck cardCounts")
        void testDeleteUserCard_Success() throws Exception {
            Deck deck = persistUserDeck(testUser, "Deck", "deck-del-card", DeckStatus.DRAFT);
            Topic topic = persistTopic(deck, "Topic", "topic-del-card", 1);
            Card card = persistCard(deck, topic, "cat", "con meo", 1);

            performDeleteCard(card.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            assertThat(cardRepository.findById(card.getId())).isEmpty();

            entityManager.flush();
            entityManager.clear();

            Topic reloadedTopic = topicRepository.findById(topic.getId()).orElseThrow();
            assertThat(reloadedTopic.getCardCount()).isEqualTo(0);

            Deck reloadedDeck = deckRepository.findById(deck.getId()).orElseThrow();
            assertThat(reloadedDeck.getCardCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Delete user card: Fail because card not found")
        void testDeleteUserCard_Fail_CardNotFound() throws Exception {
            performDeleteCard(99999L)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("CARD_NOT_FOUND"));
        }

        @Test
        @DisplayName("Delete user card: Fail because card belongs to another user")
        void testDeleteUserCard_Fail_NotOwner() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .username("other_card_deleter")
                    .email("other_deleter@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other Card Deleter")
                    .isActive(true)
                    .isVerified(true)
                    .role(Role.USER)
                    .build());
            Deck otherDeck = persistUserDeck(otherUser, "Other Deck", "other-del-deck", DeckStatus.PUBLISHED);
            Topic otherTopic = persistTopic(otherDeck, "Other Topic", "other-del-topic", 1);
            Card otherCard = persistCard(otherDeck, otherTopic, "cat", "con meo", 1);

            performDeleteCard(otherCard.getId())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Delete user card: Fail because card is in draft deck of another user")
        void testDeleteUserCard_Fail_DraftDeckNotOwner() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .username("other_card_draft_del")
                    .email("other_draft_del@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other Card Draft Del")
                    .isActive(true)
                    .isVerified(true)
                    .role(Role.USER)
                    .build());
            Deck otherDeck = persistUserDeck(otherUser, "Other Draft Deck", "other-del-draft-deck", DeckStatus.DRAFT);
            Topic otherTopic = persistTopic(otherDeck, "Other Topic", "other-del-draft-topic", 1);
            Card otherCard = persistCard(otherDeck, otherTopic, "secret", "bi mat", 1);

            performDeleteCard(otherCard.getId())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Delete user card: Fail because missing token")
        void testDeleteUserCard_Fail_Unauthenticated() throws Exception {
            mockMvc.perform(delete("/cards/{cardId}", 1L))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Delete user card: Fail because invalid token")
        void testDeleteUserCard_Fail_InvalidToken() throws Exception {
            mockMvc.perform(delete("/cards/{cardId}", 1L)
                            .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
