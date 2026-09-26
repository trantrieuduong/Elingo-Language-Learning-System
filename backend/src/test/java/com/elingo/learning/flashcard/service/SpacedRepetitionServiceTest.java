package com.elingo.learning.flashcard.service;

import com.elingo.learning.flashcard.entity.UserCardState;
import com.elingo.learning.flashcard.service.impl.SpacedRepetitionServiceImpl;
import com.elingo.user.entity.User;
import com.elingo.vocabulary.entity.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SpacedRepetitionServiceImpl.class)
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpacedRepetitionServiceTest {

    @Autowired// Spring sẽ tự nạp bean và inject các @Value
    private SpacedRepetitionServiceImpl spacedRepetitionService;

    private UserCardState state;

    @Value("${app.flashcard.srs.ef-min}")
    private BigDecimal efMin;

    @Value("${app.flashcard.srs.again-review-minutes}")
    private int againReviewMinutes;

    @BeforeEach
    void setUp() {
        User testUser = User.builder().id(1L).build();
        Card testCard = Card.builder().id(100L).build();

        state = UserCardState.builder()
                .user(testUser)
                .card(testCard)
                .srsEaseFactor(new BigDecimal("2.50"))
                .srsInterval(0)
                .build();
    }

    @Nested
    @DisplayName("calculateNextSRS - Grade 0 (Again)")
    class CalculateNextSRS_AgainTests {

        @Test
        @DisplayName("Grade 0: Should decrease EF and set interval to 0")
        void calculateNextSRS_Grade0_DecreaseEF() {
            LocalDateTime beforeCalculation = LocalDateTime.now();

            spacedRepetitionService.calculateNextSRS(state, 0);

            assertThat(state.getSrsLastGrade()).isEqualTo((short) 0);
            assertThat(state.getSrsInterval()).isEqualTo(0);
            assertThat(state.getSrsEaseFactor()).isEqualTo(new BigDecimal("2.18")); // 2.50 - 0.32

            // Interval = 0 -> nextReviewAt = now + 10 mins
            assertThat(state.getSrsNextReviewAt()).isBeforeOrEqualTo(beforeCalculation.plusMinutes(againReviewMinutes + 5));// + 5 trừ hao thời gian test chạy
        }

        @Test
        @DisplayName("Grade 0: Should not decrease EF below efMin")
        void calculateNextSRS_Grade0_MinEF() {
            state.setSrsEaseFactor(new BigDecimal("1.40")); // 1.40 - 0.32 = 1.08 < 1.30

            spacedRepetitionService.calculateNextSRS(state, 0);

            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(efMin);
            assertThat(state.getSrsInterval()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("calculateNextSRS - Grade 1 (Hard)")
    class CalculateNextSRS_HardTests {

        @Test
        @DisplayName("Grade 1 (New Card): Should decrease EF and set interval to 1")
        void calculateNextSRS_Grade1_NewCard() {
            spacedRepetitionService.calculateNextSRS(state, 1);

            assertThat(state.getSrsLastGrade()).isEqualTo((short) 1);
            assertThat(state.getSrsInterval()).isEqualTo(1);
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.36")); // 2.50 - 0.14

            // Interval = 1 -> targetDate = now + 1 day at midnight
            LocalDateTime expectedNextReview = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT);
            assertThat(state.getSrsNextReviewAt()).isEqualTo(expectedNextReview);
        }

        @Test
        @DisplayName("Grade 1 (Review Card): Should apply hard interval factor")
        void calculateNextSRS_Grade1_ReviewCard() {
            state.setSrsInterval(10); // prevInterval > 0

            spacedRepetitionService.calculateNextSRS(state, 1);

            assertThat(state.getSrsLastGrade()).isEqualTo((short) 1);
            // 10 * 1.2 = 12
            assertThat(state.getSrsInterval()).isEqualTo(12);
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.36")); // 2.50 - 0.14

            LocalDateTime expectedNextReview = LocalDateTime.of(LocalDate.now().plusDays(12), LocalTime.MIDNIGHT);
            assertThat(state.getSrsNextReviewAt()).isEqualTo(expectedNextReview);
        }
    }

    @Nested
    @DisplayName("calculateNextSRS - Grade 2 (Good)")
    class CalculateNextSRS_GoodTests {

        @Test
        @DisplayName("Grade 2 (New Card): Should maintain EF and set interval to 3")
        void calculateNextSRS_Grade2_NewCard() {
            spacedRepetitionService.calculateNextSRS(state, 2);

            assertThat(state.getSrsLastGrade()).isEqualTo((short) 2);
            assertThat(state.getSrsInterval()).isEqualTo(3);
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.50")); // EF unchanged

            LocalDateTime expectedNextReview = LocalDateTime.of(LocalDate.now().plusDays(3), LocalTime.MIDNIGHT);
            assertThat(state.getSrsNextReviewAt()).isEqualTo(expectedNextReview);
        }

        @Test
        @DisplayName("Grade 2 (Review Card): Should multiply interval by EF")
        void calculateNextSRS_Grade2_ReviewCard() {
            state.setSrsInterval(10);

            spacedRepetitionService.calculateNextSRS(state, 2);

            assertThat(state.getSrsLastGrade()).isEqualTo((short) 2);
            // 10 * 2.50 = 25
            assertThat(state.getSrsInterval()).isEqualTo(25);
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.50"));

            LocalDateTime expectedNextReview = LocalDateTime.of(LocalDate.now().plusDays(25), LocalTime.MIDNIGHT);
            assertThat(state.getSrsNextReviewAt()).isEqualTo(expectedNextReview);
        }
    }

    @Nested
    @DisplayName("calculateNextSRS - Grade 3 (Easy)")
    class CalculateNextSRS_EasyTests {

        @Test
        @DisplayName("Grade 3 (New Card): Should increase EF and set interval to 5")
        void calculateNextSRS_Grade3_NewCard() {
            spacedRepetitionService.calculateNextSRS(state, 3);

            assertThat(state.getSrsLastGrade()).isEqualTo((short) 3);
            assertThat(state.getSrsInterval()).isEqualTo(5);
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.6")); // 2.5 + 0.1

            LocalDateTime expectedNextReview = LocalDateTime.of(LocalDate.now().plusDays(5), LocalTime.MIDNIGHT);
            assertThat(state.getSrsNextReviewAt()).isEqualTo(expectedNextReview);
        }

        @Test
        @DisplayName("Grade 3 (Review Card): Should apply easy interval factor")
        void calculateNextSRS_Grade3_ReviewCard() {
            state.setSrsInterval(10);

            spacedRepetitionService.calculateNextSRS(state, 3);

            assertThat(state.getSrsLastGrade()).isEqualTo((short) 3);
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.6")); // 2.5 + 0.1
            // 10 * 2.6 * 1.30 = 33.8 -> 34
            assertThat(state.getSrsInterval()).isEqualTo(34);

            LocalDateTime expectedNextReview = LocalDateTime.of(LocalDate.now().plusDays(34), LocalTime.MIDNIGHT);
            assertThat(state.getSrsNextReviewAt()).isEqualTo(expectedNextReview);
        }
    }
}
