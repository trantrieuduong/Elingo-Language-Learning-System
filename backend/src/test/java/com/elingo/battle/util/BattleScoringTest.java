package com.elingo.battle.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class BattleScoringTest {

    private static final long TIME_LIMIT_MS = 15000;

    // ========================================================================
    // points
    // ========================================================================
    @Nested
    @DisplayName("points")
    class PointsTests {

        @Test
        @DisplayName("Wrong answer gives zero points regardless of speed")
        void points_Wrong() {
            assertThat(BattleScoring.points(false, 0, TIME_LIMIT_MS)).isZero();
            assertThat(BattleScoring.points(false, 5000, TIME_LIMIT_MS)).isZero();
            assertThat(BattleScoring.points(false, TIME_LIMIT_MS, TIME_LIMIT_MS)).isZero();
        }

        @Test
        @DisplayName("Instant correct answer gives base points plus the maximum speed bonus")
        void points_CorrectInstantly() {
            assertThat(BattleScoring.points(true, 0, TIME_LIMIT_MS))
                    .isEqualTo(BattleScoring.BASE_POINTS + BattleScoring.MAX_SPEED_BONUS);
        }

        @Test
        @DisplayName("Correct answer at the time limit gives only base points")
        void points_CorrectAtTimeLimit() {
            assertThat(BattleScoring.points(true, TIME_LIMIT_MS, TIME_LIMIT_MS)).isEqualTo(BattleScoring.BASE_POINTS);
        }

        @Test
        @DisplayName("Correct answer after the time limit gives only base points")
        void points_CorrectAfterTimeLimit() {
            assertThat(BattleScoring.points(true, TIME_LIMIT_MS + 5000, TIME_LIMIT_MS)).isEqualTo(BattleScoring.BASE_POINTS);
        }

        @Test
        @DisplayName("Speed bonus decreases linearly with the response time")
        void points_SpeedBonusIsLinear() {
            assertThat(BattleScoring.points(true, 7500, TIME_LIMIT_MS)).isEqualTo(125);
            assertThat(BattleScoring.points(true, 3750, TIME_LIMIT_MS)).isEqualTo(138);
            assertThat(BattleScoring.points(true, 11250, TIME_LIMIT_MS)).isEqualTo(113);
        }

        @Test
        @DisplayName("Speed bonus is rounded to the nearest integer")
        void points_SpeedBonusRounded() {
            // 50 * 14900 / 15000 = 49.67 -> 50
            assertThat(BattleScoring.points(true, 100, TIME_LIMIT_MS)).isEqualTo(150);
            // 50 * 100 / 15000 = 0.33 -> 0
            assertThat(BattleScoring.points(true, 14900, TIME_LIMIT_MS)).isEqualTo(100);
        }

        @Test
        @DisplayName("Faster answers never score less than slower ones")
        void points_Monotonic() {
            int previous = Integer.MAX_VALUE;
            for (long responseMs = 0; responseMs <= TIME_LIMIT_MS; responseMs += 500) {
                int points = BattleScoring.points(true, responseMs, TIME_LIMIT_MS);
                assertThat(points).isLessThanOrEqualTo(previous);
                assertThat(points).isBetween(BattleScoring.BASE_POINTS, BattleScoring.BASE_POINTS + BattleScoring.MAX_SPEED_BONUS);
                previous = points;
            }
        }

        @Test
        @DisplayName("Speed bonus scales with the question time limit")
        void points_DifferentTimeLimit() {
            assertThat(BattleScoring.points(true, 5000, 10000)).isEqualTo(125);
            assertThat(BattleScoring.points(true, 5000, 20000)).isEqualTo(138);
        }
    }
}
