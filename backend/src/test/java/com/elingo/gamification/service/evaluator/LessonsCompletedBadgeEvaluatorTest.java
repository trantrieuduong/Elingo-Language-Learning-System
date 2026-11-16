package com.elingo.gamification.service.evaluator;

import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.learning.lesson.repository.UserLessonProgressRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LessonsCompletedBadgeEvaluatorTest {

    @Mock
    private UserLessonProgressRepository lessonProgressRepository;

    @InjectMocks
    private LessonsCompletedBadgeEvaluator evaluator;

    private static final long USER_ID = 1L;

    @Test
    @DisplayName("Supports LESSONS_COMPLETED condition type")
    void supportsType_ReturnsLessonsCompleted() {
        assertThat(evaluator.supportsType()).isEqualTo(BadgeConditionType.LESSONS_COMPLETED);
    }

    @Nested
    @DisplayName("getCurrentProgress")
    class GetCurrentProgressTests {

        @Test
        @DisplayName("Returns completed lessons count from repository")
        void getCurrentProgress_ReturnsCountFromRepo() {
            when(lessonProgressRepository.countCompletedLessonsByUserId(USER_ID)).thenReturn(12L);

            long progress = evaluator.getCurrentProgress(USER_ID);

            assertThat(progress).isEqualTo(12L);
            verify(lessonProgressRepository).countCompletedLessonsByUserId(USER_ID);
        }

        @Test
        @DisplayName("Returns 0 when repository returns 0")
        void getCurrentProgress_WhenNoCompletedLessons_ReturnsZero() {
            when(lessonProgressRepository.countCompletedLessonsByUserId(USER_ID)).thenReturn(0L);

            long progress = evaluator.getCurrentProgress(USER_ID);

            assertThat(progress).isZero();
            verify(lessonProgressRepository).countCompletedLessonsByUserId(USER_ID);
        }
    }

    @Nested
    @DisplayName("isEligible")
    class IsEligibleTests {

        @Test
        @DisplayName("Returns true when progress equals or exceeds condition value")
        void isEligible_WhenProgressMeetsOrExceedsCondition_ReturnsTrue() {
            Badge badge = Badge.builder()
                    .conditionValue(10)
                    .build();

            assertThat(evaluator.isEligible(10L, badge)).isTrue();
            assertThat(evaluator.isEligible(15L, badge)).isTrue();
        }

        @Test
        @DisplayName("Returns false when progress is strictly below condition value")
        void isEligible_WhenProgressBelowCondition_ReturnsFalse() {
            Badge badge = Badge.builder()
                    .conditionValue(10)
                    .build();

            assertThat(evaluator.isEligible(9L, badge)).isFalse();
            assertThat(evaluator.isEligible(0L, badge)).isFalse();
        }
    }
}
