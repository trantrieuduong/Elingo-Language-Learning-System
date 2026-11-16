package com.elingo.gamification.service.evaluator;

import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.entity.LearningStreak;
import com.elingo.gamification.repository.LearningStreakRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class StreakDaysBadgeEvaluatorTest {

    @Mock
    private LearningStreakRepository streakRepo;

    @InjectMocks
    private StreakDaysBadgeEvaluator evaluator;

    private static final long USER_ID = 1L;

    @Test
    @DisplayName("Supports STREAK_DAYS condition type")
    void supportsType_ReturnsStreakDays() {
        assertThat(evaluator.supportsType()).isEqualTo(BadgeConditionType.STREAK_DAYS);
    }

    @Nested
    @DisplayName("getCurrentProgress")
    class GetCurrentProgressTests {

        @Test
        @DisplayName("Returns longest streak when record exists")
        void getCurrentProgress_WhenRecordExists_ReturnsLongestStreak() {
            LearningStreak streak = LearningStreak.builder()
                    .userId(USER_ID)
                    .currentStreak(5)
                    .longestStreak(14)
                    .build();

            when(streakRepo.findById(USER_ID)).thenReturn(Optional.of(streak));

            long progress = evaluator.getCurrentProgress(USER_ID);

            assertThat(progress).isEqualTo(14L);
            verify(streakRepo).findById(USER_ID);
        }

        @Test
        @DisplayName("Returns 0 when streak record does not exist")
        void getCurrentProgress_WhenRecordNotExists_ReturnsZero() {
            when(streakRepo.findById(USER_ID)).thenReturn(Optional.empty());

            long progress = evaluator.getCurrentProgress(USER_ID);

            assertThat(progress).isZero();
            verify(streakRepo).findById(USER_ID);
        }
    }

    @Nested
    @DisplayName("isEligible")
    class IsEligibleTests {

        @Test
        @DisplayName("Returns true when progress equals or exceeds condition value")
        void isEligible_WhenProgressMeetsOrExceedsCondition_ReturnsTrue() {
            Badge badge = Badge.builder()
                    .conditionValue(7)
                    .build();

            assertThat(evaluator.isEligible(7L, badge)).isTrue();
            assertThat(evaluator.isEligible(10L, badge)).isTrue();
        }

        @Test
        @DisplayName("Returns false when progress is strictly below condition value")
        void isEligible_WhenProgressBelowCondition_ReturnsFalse() {
            Badge badge = Badge.builder()
                    .conditionValue(7)
                    .build();

            assertThat(evaluator.isEligible(6L, badge)).isFalse();
            assertThat(evaluator.isEligible(0L, badge)).isFalse();
        }
    }
}
