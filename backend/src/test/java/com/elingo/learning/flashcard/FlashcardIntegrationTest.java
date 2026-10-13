package com.elingo.learning.flashcard;

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
import com.elingo.premium.entity.PremiumPlan;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.repository.PremiumPlanRepository;
import com.elingo.premium.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FlashcardIntegrationTest extends BaseIntegrationTest {
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
    private UserSubscriptionRepository userSubscriptionRepository;

    @Autowired
    private PremiumPlanRepository premiumPlanRepository;

    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .username("flashcard_tester")
                .email("flashcard_tester@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Flashcard Tester")
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
                .title("SRS Deck")
                .slug("srs-deck")
                .status(DeckStatus.PUBLISHED)
                .ownerType(OwnerType.SYSTEM)
                .build());
    }

    private Deck persistDeckWithStatusAndPremium(String title, String slug, DeckStatus status, boolean isPremium) {
        return deckRepository.save(Deck.builder()
                .title(title)
                .slug(slug)
                .status(status)
                .isPremium(isPremium)
                .ownerType(OwnerType.SYSTEM)
                .build());
    }

    private Topic persistTopic(Deck deck) {
        return topicRepository.save(Topic.builder()
                .deck(deck)
                .name("SRS Topic")
                .slug("srs-topic")
                .build());
    }

    private Card persistCard(Deck deck, Topic topic) {
        return cardRepository.save(Card.builder()
                .deck(deck)
                .topic(topic)
                .term("hello")
                .translation("xin chao")
                .order(1)
                .build());
    }

    private void persistActiveSubscription(User user) {
        PremiumPlan plan = premiumPlanRepository.save(PremiumPlan.builder()
                .name("Pro Monthly")
                .price(new BigDecimal("99000.00"))
                .currency("VND")
                .durationDays(30)
                .isActive(true)
                .build());

        userSubscriptionRepository.save(UserSubscription.builder()
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startAt(LocalDateTime.now().minusDays(5))
                .endAt(LocalDateTime.now().plusDays(25))
                .build());
    }

    private void persistExpiredSubscription(User user) {
        PremiumPlan plan = premiumPlanRepository.save(PremiumPlan.builder()
                .name("Pro Expired")
                .price(new BigDecimal("99000.00"))
                .currency("VND")
                .durationDays(30)
                .isActive(true)
                .build());

        userSubscriptionRepository.save(UserSubscription.builder()
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startAt(LocalDateTime.now().minusDays(35))
                .endAt(LocalDateTime.now().minusDays(5))
                .build());
    }


    @Nested
    @DisplayName("submitSrsReview")
    class SubmitSrsReviewTests {
        private Deck deck;
        private Topic topic;
        private Card card;

        @BeforeEach
        void setUp() {
            deck = persistDeck();
            topic = persistTopic(deck);
            card = persistCard(deck, topic);
        }

        private void persistExistingState(int interval, BigDecimal easeFactor) {
            userCardStateRepository.save(
                    UserCardState.builder()
                            .user(testUser).card(card).deck(deck).topic(topic)
                            .srsInterval(interval)
                            .srsEaseFactor(easeFactor)
                            .build());
        }

        /**
         * PATCH /flashcard/{cardId}/srs with valid auth and a numeric grade.
         */
        private ResultActions performSrsReview(int grade) throws Exception {
            return mockMvc.perform(patch("/flashcards/{cardId}/srs", card.getId())
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"grade\": " + grade + "}"));
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade AGAIN creates new state with interval zero and decreased EF")
        void testSubmitSrsReview_Again_NewState_IntervalZero() throws Exception {
            performSrsReview(0)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId())
                    .orElseThrow(() -> new AssertionError("UserCardState must be created"));

            assertThat(state.getSrsInterval()).isEqualTo(0);
            assertThat(state.getSrsLastGrade()).isEqualTo((short) 0);

            // EF ban dau = 2.5, again-ef-penalty = -0.32 -> 2.18
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.18"));
            // isEqualByComparingTo: so sánh giá trị toán học 2.18 = 2.180

            assertThat(state.getSrsNextReviewAt())
                    .isAfter(LocalDateTime.now())
                    .isBefore(LocalDateTime.now().plusMinutes(15));
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade HARD creates new state with interval one and decreased EF")
        void testSubmitSrsReview_Hard_NewState_IntervalOne() throws Exception {
            performSrsReview(1)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId())
                    .orElseThrow();

            assertThat(state.getSrsInterval()).isEqualTo(1);
            assertThat(state.getSrsLastGrade()).isEqualTo((short) 1);

            // EF ban dau = 2.5, hard-ef-penalty = -0.14 -> 2.36
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.36"));

            assertThat(state.getSrsNextReviewAt()).isCloseTo(LocalDateTime.now().plusDays(1), within(1, ChronoUnit.SECONDS));
            // Cho phép lệch tối đa 1 giây
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade GOOD creates new state with interval three and kept EF")
        void testSubmitSrsReview_Good_NewState_IntervalThree() throws Exception {
            performSrsReview(2)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId())
                    .orElseThrow();

            assertThat(state.getSrsInterval()).isEqualTo(3);
            assertThat(state.getSrsLastGrade()).isEqualTo((short) 2);

            // EF ban dau = 2.5
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.5"));

            assertThat(state.getSrsNextReviewAt())
                    .isCloseTo(LocalDateTime.now().plusDays(3), within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade EASY creates new state with interval five and increased EF")
        void testSubmitSrsReview_Easy_NewState_IntervalFive_EFIncreased() throws Exception {
            performSrsReview(3)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId())
                    .orElseThrow();

            assertThat(state.getSrsInterval()).isEqualTo(5);
            assertThat(state.getSrsLastGrade()).isEqualTo((short) 3);

            // EF ban dau = 2.5, easy-ef-bonus = +0.1 -> 2.6
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.6"));

            assertThat(state.getSrsNextReviewAt().toLocalDate())
                    .isEqualTo(LocalDate.now().plusDays(5));
        }

        @Test
        @DisplayName("Submit SRS review: Success - update existing state without duplicate")
        void testSubmitSrsReview_UpdatesExistingState_NoDuplicate() throws Exception {
            persistExistingState(3, new BigDecimal("2.50"));

            // GOOD(2) lan 2: prevInterval=3, newInterval = round half up(3 * 2.50) = 8
            performSrsReview(2)
                    .andExpect(status().isOk());

            assertThat(userCardStateRepository.count()).isEqualTo(1);
            UserCardState updated = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId()).orElseThrow();
            assertThat(updated.getSrsInterval()).isEqualTo(8);
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade AGAIN after learned resets interval and reduces EF")
        void testSubmitSrsReview_Again_AfterLearned_ResetsIntervalAndReducesEF() throws Exception {
            persistExistingState(3, new BigDecimal("2.50"));

            performSrsReview(0)
                    .andExpect(status().isOk());

            UserCardState updated = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId()).orElseThrow();

            assertThat(updated.getSrsInterval()).isEqualTo(0);

            // EF: 2.50 - 0.32 = 2.18
            assertThat(updated.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.18"));
        }

        @Test
        @DisplayName("Submit SRS review: Success - EF never drops below minimum")
        void testSubmitSrsReview_Again_EFNeverDropsBelowMinimum() throws Exception {
            persistExistingState(0, new BigDecimal("1.35"));

            performSrsReview(0)
                    .andExpect(status().isOk());

            UserCardState updated = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId()).orElseThrow();

            assertThat(updated.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("1.3"));
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade HARD after learned applies hard interval factor")
        void testSubmitSrsReview_Hard_AfterLearned_AppliesHardIntervalFactor() throws Exception {
            persistExistingState(5, new BigDecimal("2.50"));

            performSrsReview(1)
                    .andExpect(status().isOk());

            UserCardState updated = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId()).orElseThrow();

            // round hafl up(5 * 1.2) = 6
            assertThat(updated.getSrsInterval()).isEqualTo(6);
            // EF: 2.50 - 0.14 = 2.36
            assertThat(updated.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.36"));
        }

        @Test
        @DisplayName("Submit SRS review: Success - grade EASY after learned applies easy interval factor")
        void testSubmitSrsReview_Easy_AfterLearned_AppliesEasyIntervalFactor() throws Exception {
            persistExistingState(5, new BigDecimal("2.50"));

            performSrsReview(3)
                    .andExpect(status().isOk());

            UserCardState updated = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId()).orElseThrow();

            // newEF = 2.5 + 0.1 = 2.6
            assertThat(updated.getSrsEaseFactor()).isEqualByComparingTo("2.6");
            // newInterval = round half up(5 * 2.6 * 1.3) = 17
            assertThat(updated.getSrsInterval()).isEqualTo(17);
        }

        @Test
        @DisplayName("Submit SRS review: Fail because of null grade")
        void testSubmitSrsReview_Fail_NullGrade() throws Exception {
            mockMvc.perform(patch("/flashcards/{cardId}/srs", card.getId())
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grade\": null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_SRS_GRADE"));
        }

        @Test
        @DisplayName("Submit SRS review: Fail because of grade too high")
        void testSubmitSrsReview_Fail_GradeTooHigh() throws Exception {
            performSrsReview(4)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_SRS_GRADE"));
        }

        @Test
        @DisplayName("Submit SRS review: Fail because of negative grade")
        void testSubmitSrsReview_Fail_GradeNegative() throws Exception {
            performSrsReview(-1)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_SRS_GRADE"));
        }

        @Test
        @DisplayName("Submit SRS review: Fail because of empty body")
        void testSubmitSrsReview_Fail_EmptyBody() throws Exception {
            mockMvc.perform(patch("/flashcards/{cardId}/srs", card.getId())
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_SRS_GRADE"));
        }

        @Test
        @DisplayName("Submit SRS review: Fail because deck is unpublished (DRAFT)")
        void testSubmitSrsReview_Fail_UnpublishedDeck_Draft() throws Exception {
            Deck draftDeck = persistDeckWithStatusAndPremium("Draft Deck", "draft-deck", DeckStatus.DRAFT, false);
            Topic draftTopic = persistTopic(draftDeck);
            Card draftCard = persistCard(draftDeck, draftTopic);

            mockMvc.perform(patch("/flashcards/{cardId}/srs", draftCard.getId())
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grade\": 3}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Submit SRS review: Fail because deck is unpublished (ARCHIVED)")
        void testSubmitSrsReview_Fail_UnpublishedDeck_Archived() throws Exception {
            Deck archivedDeck = persistDeckWithStatusAndPremium("Archived Deck", "archived-deck", DeckStatus.ARCHIVED, false);
            Topic archivedTopic = persistTopic(archivedDeck);
            Card archivedCard = persistCard(archivedDeck, archivedTopic);

            mockMvc.perform(patch("/flashcards/{cardId}/srs", archivedCard.getId())
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grade\": 3}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Submit SRS review: Fail because premium deck without active subscription")
        void testSubmitSrsReview_Fail_PremiumDeck_NoSubscription() throws Exception {
            Deck premiumDeck = persistDeckWithStatusAndPremium("Premium Deck", "premium-deck", DeckStatus.PUBLISHED, true);
            Topic premiumTopic = persistTopic(premiumDeck);
            Card premiumCard = persistCard(premiumDeck, premiumTopic);

            mockMvc.perform(patch("/flashcards/{cardId}/srs", premiumCard.getId())
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grade\": 3}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_PREMIUM_REQUIRED"));
        }

        @Test
        @DisplayName("Submit SRS review: Fail because premium deck with expired subscription")
        void testSubmitSrsReview_Fail_PremiumDeck_ExpiredSubscription() throws Exception {
            Deck premiumDeck = persistDeckWithStatusAndPremium("Premium Deck Expired", "premium-deck-expired", DeckStatus.PUBLISHED, true);
            Topic premiumTopic = persistTopic(premiumDeck);
            Card premiumCard = persistCard(premiumDeck, premiumTopic);
            persistExpiredSubscription(testUser);

            mockMvc.perform(patch("/flashcards/{cardId}/srs", premiumCard.getId())
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grade\": 3}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_PREMIUM_REQUIRED"));
        }

        @Test
        @DisplayName("Submit SRS review: Success for premium deck with active subscription")
        void testSubmitSrsReview_Success_PremiumDeck_WithActiveSubscription() throws Exception {
            Deck premiumDeck = persistDeckWithStatusAndPremium("Premium Deck Active", "premium-deck-active", DeckStatus.PUBLISHED, true);
            Topic premiumTopic = persistTopic(premiumDeck);
            Card premiumCard = persistCard(premiumDeck, premiumTopic);
            persistActiveSubscription(testUser);

            mockMvc.perform(patch("/flashcards/{cardId}/srs", premiumCard.getId())
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grade\": 3}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(premiumCard.getId(), testUser.getId())
                    .orElseThrow();
            assertThat(state.getSrsInterval()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("getCardsForReview")
    class GetCardsForReviewTests {
        private Deck deck;
        private Topic topic;

        @BeforeEach
        void setUp() {
            deck = persistDeck();
            topic = persistTopic(deck);
        }

        @Test
        @DisplayName("Get cards for review: only returns due cards (srsNextReviewAt <= now) that are not hidden")
        void testGetCardsForReview_FiltersDueAndNotHidden() throws Exception {
            Card dueCard = persistCard(deck, topic);
            Card futureCard = persistCard(deck, topic);
            Card hiddenCard = persistCard(deck, topic);
            Card unlearnedCard = persistCard(deck, topic);

            userCardStateRepository.save(UserCardState.builder()
                    .user(testUser).card(dueCard).deck(deck).topic(topic)
                    .srsNextReviewAt(LocalDateTime.now().minusHours(1))
                    .flagsHidden(false)
                    .build());

            userCardStateRepository.save(UserCardState.builder()
                    .user(testUser).card(futureCard).deck(deck).topic(topic)
                    .srsNextReviewAt(LocalDateTime.now().plusDays(1))
                    .flagsHidden(false)
                    .build());

            userCardStateRepository.save(UserCardState.builder()
                    .user(testUser).card(hiddenCard).deck(deck).topic(topic)
                    .srsNextReviewAt(LocalDateTime.now().minusHours(1))
                    .flagsHidden(true)
                    .build());

            userCardStateRepository.save(UserCardState.builder()
                    .user(testUser).card(unlearnedCard).deck(deck).topic(topic)
                    .srsNextReviewAt(null)
                    .flagsHidden(false)
                    .build());

            mockMvc.perform(get("/flashcards/reviews")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].card.id").value(dueCard.getId()));
        }

        @Test
        @DisplayName("Get cards for review: excludes cards from unpublished decks (DRAFT / ARCHIVED)")
        void testGetCardsForReview_ExcludesCardsFromUnpublishedDecks() throws Exception {
            Deck draftDeck = persistDeckWithStatusAndPremium("Draft Deck Rev", "draft-deck-rev", DeckStatus.DRAFT, false);
            Topic draftTopic = persistTopic(draftDeck);
            Card draftCard = persistCard(draftDeck, draftTopic);

            Deck archivedDeck = persistDeckWithStatusAndPremium("Archived Deck Rev", "archived-deck-rev", DeckStatus.ARCHIVED, false);
            Topic archivedTopic = persistTopic(archivedDeck);
            Card archivedCard = persistCard(archivedDeck, archivedTopic);

            userCardStateRepository.save(UserCardState.builder()
                    .user(testUser).card(draftCard).deck(draftDeck).topic(draftTopic)
                    .srsNextReviewAt(LocalDateTime.now().minusHours(1))
                    .flagsHidden(false)
                    .build());

            userCardStateRepository.save(UserCardState.builder()
                    .user(testUser).card(archivedCard).deck(archivedDeck).topic(archivedTopic)
                    .srsNextReviewAt(LocalDateTime.now().minusHours(2))
                    .flagsHidden(false)
                    .build());

            mockMvc.perform(get("/flashcards/reviews")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("Get cards for review: excludes cards belonging to other users")
        void testGetCardsForReview_ExcludesCardsFromOtherUsers() throws Exception {
            Card dueCardOtherUser = persistCard(deck, topic);
            User otherUser = userRepository.save(User.builder()
                    .username("other_reviewer")
                    .email("other_reviewer@elingo.test")
                    .passwordHash(passwordEncoder.encode("Password123@"))
                    .fullName("Other Reviewer")
                    .isActive(true)
                    .isVerified(true)
                    .build());

            userCardStateRepository.save(UserCardState.builder()
                    .user(otherUser).card(dueCardOtherUser).deck(deck).topic(topic)
                    .srsNextReviewAt(LocalDateTime.now().minusHours(1))
                    .flagsHidden(false)
                    .build());

            mockMvc.perform(get("/flashcards/reviews")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("Get cards for review: orders cards by srsNextReviewAt ascending (oldest overdue first)")
        void testGetCardsForReview_OrdersBySrsNextReviewAtAscending() throws Exception {
            Card cardOverdueMore = persistCard(deck, topic);
            Card cardOverdueLess = persistCard(deck, topic);

            userCardStateRepository.save(UserCardState.builder()
                    .user(testUser).card(cardOverdueMore).deck(deck).topic(topic)
                    .srsNextReviewAt(LocalDateTime.now().minusHours(5))
                    .flagsHidden(false)
                    .build());

            userCardStateRepository.save(UserCardState.builder()
                    .user(testUser).card(cardOverdueLess).deck(deck).topic(topic)
                    .srsNextReviewAt(LocalDateTime.now().minusHours(1))
                    .flagsHidden(false)
                    .build());

            mockMvc.perform(get("/flashcards/reviews")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].card.id").value(cardOverdueMore.getId()))
                    .andExpect(jsonPath("$.data[1].card.id").value(cardOverdueLess.getId()));
        }
    }

    @Nested
    @DisplayName("toggleStar")
    class ToggleStarTests {
        private Card card;

        @BeforeEach
        void setUp() {
            Deck deck = persistDeck();
            Topic topic = persistTopic(deck);
            card = persistCard(deck, topic);
        }

        @Test
        @DisplayName("Toggle star: Success for normal published deck")
        void testToggleStar_Success() throws Exception {
            mockMvc.perform(patch("/flashcards/{cardId}/stars", card.getId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId()).orElseThrow();
            assertThat(state.getFlagsStarred()).isTrue();
        }

        @Test
        @DisplayName("Toggle star: Fail because deck is unpublished (DRAFT)")
        void testToggleStar_Fail_UnpublishedDeck() throws Exception {
            Deck draftDeck = persistDeckWithStatusAndPremium("Draft Deck Star", "draft-deck-star", DeckStatus.DRAFT, false);
            Topic draftTopic = persistTopic(draftDeck);
            Card draftCard = persistCard(draftDeck, draftTopic);

            mockMvc.perform(patch("/flashcards/{cardId}/stars", draftCard.getId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Toggle star: Fail because premium deck without active subscription")
        void testToggleStar_Fail_PremiumDeck_NoSubscription() throws Exception {
            Deck premiumDeck = persistDeckWithStatusAndPremium("Premium Deck Star", "premium-deck-star", DeckStatus.PUBLISHED, true);
            Topic premiumTopic = persistTopic(premiumDeck);
            Card premiumCard = persistCard(premiumDeck, premiumTopic);

            mockMvc.perform(patch("/flashcards/{cardId}/stars", premiumCard.getId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_PREMIUM_REQUIRED"));
        }

        @Test
        @DisplayName("Toggle star: Success for premium deck with active subscription")
        void testToggleStar_Success_PremiumDeck_WithActiveSubscription() throws Exception {
            Deck premiumDeck = persistDeckWithStatusAndPremium("Premium Deck Star Active", "premium-deck-star-active", DeckStatus.PUBLISHED, true);
            Topic premiumTopic = persistTopic(premiumDeck);
            Card premiumCard = persistCard(premiumDeck, premiumTopic);
            persistActiveSubscription(testUser);

            mockMvc.perform(patch("/flashcards/{cardId}/stars", premiumCard.getId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(premiumCard.getId(), testUser.getId()).orElseThrow();
            assertThat(state.getFlagsStarred()).isTrue();
        }
    }

    @Nested
    @DisplayName("toggleHide")
    class ToggleHideTests {
        private Card card;

        @BeforeEach
        void setUp() {
            Deck deck = persistDeck();
            Topic topic = persistTopic(deck);
            card = persistCard(deck, topic);
        }

        @Test
        @DisplayName("Toggle hide: Success for normal published deck")
        void testToggleHide_Success() throws Exception {
            mockMvc.perform(patch("/flashcards/{cardId}/hidden", card.getId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(card.getId(), testUser.getId()).orElseThrow();
            assertThat(state.getFlagsHidden()).isTrue();
        }

        @Test
        @DisplayName("Toggle hide: Fail because deck is unpublished (DRAFT)")
        void testToggleHide_Fail_UnpublishedDeck() throws Exception {
            Deck draftDeck = persistDeckWithStatusAndPremium("Draft Deck Hide", "draft-deck-hide", DeckStatus.DRAFT, false);
            Topic draftTopic = persistTopic(draftDeck);
            Card draftCard = persistCard(draftDeck, draftTopic);

            mockMvc.perform(patch("/flashcards/{cardId}/hidden", draftCard.getId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Toggle hide: Fail because premium deck without active subscription")
        void testToggleHide_Fail_PremiumDeck_NoSubscription() throws Exception {
            Deck premiumDeck = persistDeckWithStatusAndPremium("Premium Deck Hide", "premium-deck-hide", DeckStatus.PUBLISHED, true);
            Topic premiumTopic = persistTopic(premiumDeck);
            Card premiumCard = persistCard(premiumDeck, premiumTopic);

            mockMvc.perform(patch("/flashcards/{cardId}/hidden", premiumCard.getId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_PREMIUM_REQUIRED"));
        }

        @Test
        @DisplayName("Toggle hide: Success for premium deck with active subscription")
        void testToggleHide_Success_PremiumDeck_WithActiveSubscription() throws Exception {
            Deck premiumDeck = persistDeckWithStatusAndPremium("Premium Deck Hide Active", "premium-deck-hide-active", DeckStatus.PUBLISHED, true);
            Topic premiumTopic = persistTopic(premiumDeck);
            Card premiumCard = persistCard(premiumDeck, premiumTopic);
            persistActiveSubscription(testUser);

            mockMvc.perform(patch("/flashcards/{cardId}/hidden", premiumCard.getId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            UserCardState state = userCardStateRepository
                    .findByCardIdAndUserId(premiumCard.getId(), testUser.getId()).orElseThrow();
            assertThat(state.getFlagsHidden()).isTrue();
        }
    }
}