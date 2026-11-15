package com.elingo.learning.dictation.service;

import com.elingo.learning.dictation.service.impl.DictationScoringServiceImpl;
import com.elingo.learning.lesson.config.LessonProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class DictationScoringServiceTest {

    private DictationScoringServiceImpl scoringService;

    @BeforeEach
    void setUp() {
        // LessonProperties là record nên tạo trực tiếp: dictationAttemptPenalty = 5
        scoringService = new DictationScoringServiceImpl(new LessonProperties(60, 55, 5, 10));
    }

    // ========================================================================
    // calculateScore
    // ========================================================================
    @Nested
    @DisplayName("calculateScore")
    class CalculateScoreTests {

        @Test
        @DisplayName("Perfect score on the first attempt without hints")
        void calculateScore_Success_PerfectScore() {
            assertThat(scoringService.calculateScore(1, 0, 10)).isEqualTo(100);
        }

        @Test
        @DisplayName("Each extra attempt costs the attempt penalty")
        void calculateScore_Success_AttemptPenalty() {
            assertThat(scoringService.calculateScore(2, 0, 10)).isEqualTo(95);
            assertThat(scoringService.calculateScore(3, 0, 10)).isEqualTo(90);
            assertThat(scoringService.calculateScore(11, 0, 10)).isEqualTo(50);
        }

        @Test
        @DisplayName("Hints reduce the score proportionally to the number of words")
        void calculateScore_Success_HintPenalty() {
            assertThat(scoringService.calculateScore(1, 1, 10)).isEqualTo(90);
            assertThat(scoringService.calculateScore(1, 5, 10)).isEqualTo(50);
            assertThat(scoringService.calculateScore(1, 10, 10)).isZero();
        }

        @Test
        @DisplayName("Attempt and hint penalties are combined")
        void calculateScore_Success_AttemptAndHintPenalty() {
            // 100 - 2 * 5 - 20 = 70
            assertThat(scoringService.calculateScore(3, 2, 10)).isEqualTo(70);
        }

        @Test
        @DisplayName("Score is rounded to the nearest integer")
        void calculateScore_Success_Rounding() {
            // 100 - 33.33 = 66.67 -> 67
            assertThat(scoringService.calculateScore(1, 1, 3)).isEqualTo(67);
            // 100 - 5 - 33.33 = 61.67 -> 62
            assertThat(scoringService.calculateScore(2, 1, 3)).isEqualTo(62);
            // 100 - 12.5 = 87.5 -> 88
            assertThat(scoringService.calculateScore(1, 1, 8)).isEqualTo(88);
        }

        @Test
        @DisplayName("Hint count larger than total words is capped at total words")
        void calculateScore_Success_HintCappedAtTotalWords() {
            assertThat(scoringService.calculateScore(1, 50, 10)).isZero();
            assertThat(scoringService.calculateScore(1, 11, 10)).isEqualTo(scoringService.calculateScore(1, 10, 10));
        }

        @Test
        @DisplayName("No hint penalty when transcript has no words")
        void calculateScore_Success_ZeroTotalWords() {
            assertThat(scoringService.calculateScore(1, 5, 0)).isEqualTo(100);
            assertThat(scoringService.calculateScore(3, 5, 0)).isEqualTo(90);
        }

        @Test
        @DisplayName("Score never goes below zero")
        void calculateScore_Success_NeverNegative() {
            assertThat(scoringService.calculateScore(1000, 0, 10)).isZero();
            assertThat(scoringService.calculateScore(30, 10, 10)).isZero();
        }

        @Test
        @DisplayName("Score never exceeds 100")
        void calculateScore_Success_NeverAbove100() {
            // attemptCount < 1 không hợp lệ nhưng điểm vẫn bị chặn ở 100
            assertThat(scoringService.calculateScore(0, 0, 10)).isEqualTo(100);
        }

        @Test
        @DisplayName("Attempt penalty follows the configuration")
        void calculateScore_Success_CustomPenalty() {
            DictationScoringServiceImpl custom = new DictationScoringServiceImpl(new LessonProperties(60, 55, 20, 10));

            assertThat(custom.calculateScore(3, 0, 10)).isEqualTo(60);
        }
    }
}
