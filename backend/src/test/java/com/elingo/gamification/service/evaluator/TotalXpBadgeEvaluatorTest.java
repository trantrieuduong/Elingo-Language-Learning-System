package com.elingo.gamification.service.evaluator;

import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.entity.UserXp;
import com.elingo.gamification.repository.UserXpRepository;
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
class TotalXpBadgeEvaluatorTest {

    @Mock
    private UserXpRepository userXpRepo;

    @InjectMocks
    private TotalXpBadgeEvaluator evaluator;

    private static final long USER_ID = 1L;

    @Test
    @DisplayName("Supports TOTAL_XP condition type")
    void supportsType_ReturnsTotalXp() {
        assertThat(evaluator.supportsType()).isEqualTo(BadgeConditionType.TOTAL_XP);
    }

    @Nested
    @DisplayName("getCurrentProgress")
    class GetCurrentProgressTests {

        @Test
        @DisplayName("Returns total XP when record exists")
        void getCurrentProgress_WhenRecordExists_ReturnsTotalXp() {
            UserXp userXp = UserXp.builder()
                    .userId(USER_ID)
                    .totalXp(1500)
                    .build();

            when(userXpRepo.findById(USER_ID)).thenReturn(Optional.of(userXp));

            long progress = evaluator.getCurrentProgress(USER_ID);

            assertThat(progress).isEqualTo(1500L);
            verify(userXpRepo).findById(USER_ID);
        }

        @Test
        @DisplayName("Returns 0 when user XP record does not exist")
        void getCurrentProgress_WhenRecordNotExists_ReturnsZero() {
            when(userXpRepo.findById(USER_ID)).thenReturn(Optional.empty());

            long progress = evaluator.getCurrentProgress(USER_ID);

            assertThat(progress).isZero();
            verify(userXpRepo).findById(USER_ID);
        }
    }

    @Nested
    @DisplayName("isEligible")
    class IsEligibleTests {

        @Test
        @DisplayName("Returns true when progress equals or exceeds condition value")
        void isEligible_WhenProgressMeetsOrExceedsCondition_ReturnsTrue() {
            Badge badge = Badge.builder()
                    .conditionValue(1000)
                    .build();

            assertThat(evaluator.isEligible(1000L, badge)).isTrue();
            assertThat(evaluator.isEligible(2500L, badge)).isTrue();
        }

        @Test
        @DisplayName("Returns false when progress is strictly below condition value")
        void isEligible_WhenProgressBelowCondition_ReturnsFalse() {
            Badge badge = Badge.builder()
                    .conditionValue(1000)
                    .build();

            assertThat(evaluator.isEligible(999L, badge)).isFalse();
            assertThat(evaluator.isEligible(0L, badge)).isFalse();
        }
    }
}
