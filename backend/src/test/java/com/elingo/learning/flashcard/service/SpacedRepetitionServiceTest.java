package com.elingo.learning.flashcard.service;

import com.elingo.config.FlashcardProperties;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpacedRepetitionServiceTest {

    private static final BigDecimal INITIAL_EF = new BigDecimal("2.50");
    private static final BigDecimal EF_MIN = new BigDecimal("1.30");
    private static final BigDecimal AGAIN_EF_PENALTY = new BigDecimal("0.32");
    private static final BigDecimal HARD_EF_PENALTY = new BigDecimal("0.14");
    private static final BigDecimal EASY_EF_BONUS = new BigDecimal("0.10");
    private static final BigDecimal HARD_INTERVAL_FACTOR = new BigDecimal("1.2");
    private static final BigDecimal EASY_INTERVAL_FACTOR = new BigDecimal("1.3");
    private static final int AGAIN_REVIEW_MINUTES = 10;

    private SpacedRepetitionServiceImpl spacedRepetitionService;
    private UserCardState state;

    @BeforeEach
    void setUp() {
        FlashcardProperties.Srs srs = new FlashcardProperties.Srs();
        srs.setEfMin(EF_MIN);
        srs.setAgainEfPenalty(AGAIN_EF_PENALTY);
        srs.setHardEfPenalty(HARD_EF_PENALTY);
        srs.setEasyEfBonus(EASY_EF_BONUS);
        srs.setHardIntervalFactor(HARD_INTERVAL_FACTOR);
        srs.setEasyIntervalFactor(EASY_INTERVAL_FACTOR);
        srs.setAgainReviewMinutes(AGAIN_REVIEW_MINUTES);

        FlashcardProperties props = new FlashcardProperties();
        props.setSrs(srs);

        spacedRepetitionService = new SpacedRepetitionServiceImpl(props);

        state = UserCardState.builder()
                .user(User.builder().id(1L).build())
                .card(Card.builder().id(100L).build())
                .srsEaseFactor(INITIAL_EF)
                .srsInterval(0)
                .build();
    }

    @Nested
    @DisplayName("calculateNextSRS - Grade 0 (Again)")
    class CalculateNextSRS_AgainTests {

        @Test
        @DisplayName("Grade 0: Should decrease EF, set interval to 0 and learn after AGAIN_REVIEW_MINUTES")
        void calculateNextSRS_Grade0_DecreaseEF() {
            spacedRepetitionService.calculateNextSRS(state, 0);

            assertThat(state.getSrsLastGrade()).isEqualTo((short) 0);
            assertThat(state.getSrsInterval()).isEqualTo(0);
            // 2.50 - 0.32 = 2.18
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.18"));
            // Interval = 0 -> nextReviewAt = now + 10 mins
            assertThat(state.getSrsNextReviewAt())
                    .isCloseTo(LocalDateTime.now().plusMinutes(AGAIN_REVIEW_MINUTES), within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Grade 0: Should not decrease EF below efMin")
        void calculateNextSRS_Grade0_MinEF() {
            // 1.40 - 0.32 = 1.08 < 1.30 -> to 1.30
            state.setSrsEaseFactor(new BigDecimal("1.40"));

            spacedRepetitionService.calculateNextSRS(state, 0);

            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(EF_MIN);
            assertThat(state.getSrsInterval()).isEqualTo(0);
        }

        @Test
        @DisplayName("Grade 0: Should keep EF to efMin when EF is already at efMin")
        void calculateNextSRS_Grade0_EfAlreadyAtMin() {
            state.setSrsEaseFactor(EF_MIN);

            spacedRepetitionService.calculateNextSRS(state, 0);

            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(EF_MIN);
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
            // 2.50 - 0.14 = 2.36
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.36"));
            // Interval = 1 -> nextReviewAt = now + 1 day
            assertThat(state.getSrsNextReviewAt())
                    .isCloseTo(LocalDateTime.now().plusDays(1), within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Grade 1 (Review Card): Should apply hard interval factor")
        void calculateNextSRS_Grade1_ReviewCard() {
            state.setSrsInterval(10); // prevInterval > 0

            spacedRepetitionService.calculateNextSRS(state, 1);

            assertThat(state.getSrsLastGrade()).isEqualTo((short) 1);
            // 10 * 1.2 = 12
            assertThat(state.getSrsInterval()).isEqualTo(12);
            // 2.50 - 0.14 = 2.36
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.36"));
            assertThat(state.getSrsNextReviewAt())
                    .isCloseTo(LocalDateTime.now().plusDays(12), within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Grade 1: Should not decrease EF below efMin")
        void calculateNextSRS_Grade1_MinEF() {
            // 1.40 - 0.14 = 1.26 < 1.30
            state.setSrsEaseFactor(new BigDecimal("1.40"));

            spacedRepetitionService.calculateNextSRS(state, 1);

            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(EF_MIN);
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
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(INITIAL_EF); // EF unchanged
            assertThat(state.getSrsNextReviewAt())
                    .isCloseTo(LocalDateTime.now().plusDays(3), within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Grade 2 (Review Card): Should multiply interval by EF")
        void calculateNextSRS_Grade2_ReviewCard() {
            state.setSrsInterval(10);

            spacedRepetitionService.calculateNextSRS(state, 2);

            assertThat(state.getSrsLastGrade()).isEqualTo((short) 2);
            // 10 * 2.50 = 25
            assertThat(state.getSrsInterval()).isEqualTo(25);
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(INITIAL_EF);
            assertThat(state.getSrsNextReviewAt())
                    .isCloseTo(LocalDateTime.now().plusDays(25), within(1, ChronoUnit.SECONDS));
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
            // 2.50 + 0.10 = 2.60
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.60"));
            assertThat(state.getSrsNextReviewAt())
                    .isCloseTo(LocalDateTime.now().plusDays(5), within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Grade 3 (Review Card): Should apply easy interval factor")
        void calculateNextSRS_Grade3_ReviewCard() {
            state.setSrsInterval(10);

            spacedRepetitionService.calculateNextSRS(state, 3);

            assertThat(state.getSrsLastGrade()).isEqualTo((short) 3);
            // 2.50 + 0.10 = 2.60
            assertThat(state.getSrsEaseFactor()).isEqualByComparingTo(new BigDecimal("2.60"));
            // 10 * 2.60 * 1.30 = 33.8 -> 34
            assertThat(state.getSrsInterval()).isEqualTo(34);
            assertThat(state.getSrsNextReviewAt())
                    .isCloseTo(LocalDateTime.now().plusDays(34), within(1, ChronoUnit.SECONDS));
        }
    }
}