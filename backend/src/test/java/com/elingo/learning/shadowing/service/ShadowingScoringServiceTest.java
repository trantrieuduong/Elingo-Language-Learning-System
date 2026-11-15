package com.elingo.learning.shadowing.service;

import com.elingo.learning.lesson.config.LessonProperties;
import com.elingo.learning.shadowing.service.impl.ShadowingScoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ShadowingScoringServiceTest {

    private ShadowingScoringServiceImpl scoringService;

    @BeforeEach
    void setUp() {
        // LessonProperties là record nên tạo trực tiếp: shadowingAttemptPenalty = 10
        scoringService = new ShadowingScoringServiceImpl(new LessonProperties(60, 55, 5, 10));
    }

    // ========================================================================
    // calculateFinalScore
    // ========================================================================
    @Nested
    @DisplayName("calculateFinalScore")
    class CalculateFinalScoreTests {

        @Test
        @DisplayName("First attempt keeps the original score")
        void calculateFinalScore_Success_FirstAttempt() {
            assertThat(scoringService.calculateFinalScore(80, 1)).isEqualTo(80);
            assertThat(scoringService.calculateFinalScore(100, 1)).isEqualTo(100);
            assertThat(scoringService.calculateFinalScore(0, 1)).isZero();
        }

        @Test
        @DisplayName("Each extra attempt costs the attempt penalty")
        void calculateFinalScore_Success_AttemptPenalty() {
            assertThat(scoringService.calculateFinalScore(80, 2)).isEqualTo(70);
            assertThat(scoringService.calculateFinalScore(80, 3)).isEqualTo(60);
            assertThat(scoringService.calculateFinalScore(100, 5)).isEqualTo(60);
        }

        @Test
        @DisplayName("Final score never goes below zero")
        void calculateFinalScore_Success_NeverNegative() {
            assertThat(scoringService.calculateFinalScore(30, 10)).isZero();
            assertThat(scoringService.calculateFinalScore(100, 1000)).isZero();
        }

        @Test
        @DisplayName("Final score is zero when penalty equals the score exactly")
        void calculateFinalScore_Success_ExactlyZero() {
            assertThat(scoringService.calculateFinalScore(20, 3)).isZero();
        }

        @Test
        @DisplayName("Attempt penalty follows the configuration")
        void calculateFinalScore_Success_CustomPenalty() {
            ShadowingScoringServiceImpl custom = new ShadowingScoringServiceImpl(new LessonProperties(60, 55, 5, 25));

            assertThat(custom.calculateFinalScore(90, 3)).isEqualTo(40);
        }
    }
}
