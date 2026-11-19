package com.elingo.gamification.service.evaluator;

import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.learning.lesson.repository.UserSegmentProgressRepository;
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
class SegmentCompletedBadgeEvaluatorTest {

    @Mock
    private UserSegmentProgressRepository segmentProgressRepository;

    @InjectMocks
    private SegmentCompletedBadgeEvaluator evaluator;

    private static final long USER_ID = 1L;

    @Test
    @DisplayName("Supports SEGMENT_COMPLETED condition type")
    void supportsType_ReturnsSegmentCompleted() {
        assertThat(evaluator.supportsType()).isEqualTo(BadgeConditionType.SEGMENT_COMPLETED);
    }

    @Nested
    @DisplayName("getCurrentProgress")
    class GetCurrentProgressTests {

        @Test
        @DisplayName("Returns completed segments count from repository")
        void getCurrentProgress_ReturnsCountFromRepo() {
            when(segmentProgressRepository.countCompletedSegmentsByUserId(USER_ID)).thenReturn(12L);

            long progress = evaluator.getCurrentProgress(USER_ID);

            assertThat(progress).isEqualTo(12L);
            verify(segmentProgressRepository).countCompletedSegmentsByUserId(USER_ID);
        }

        @Test
        @DisplayName("Returns 0 when repository returns 0")
        void getCurrentProgress_WhenNoCompletedSegments_ReturnsZero() {
            when(segmentProgressRepository.countCompletedSegmentsByUserId(USER_ID)).thenReturn(0L);

            long progress = evaluator.getCurrentProgress(USER_ID);

            assertThat(progress).isZero();
            verify(segmentProgressRepository).countCompletedSegmentsByUserId(USER_ID);
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
