package com.elingo.gamification.service.evaluator;

import com.elingo.battle.repository.BattleParticipantRepository;
import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleWinsBadgeEvaluatorTest {

    @Mock
    private BattleParticipantRepository battleParticipantRepository;

    @InjectMocks
    private BattleWinsBadgeEvaluator evaluator;

    private static final long USER_ID = 1L;

    @Test
    @DisplayName("Supports BATTLE_WINS condition type")
    void supportsType_ReturnsBattleWins() {
        assertThat(evaluator.supportsType()).isEqualTo(BadgeConditionType.BATTLE_WINS);
    }

    @Nested
    @DisplayName("getCurrentProgress")
    class GetCurrentProgressTests {

        @Test
        @DisplayName("Returns battle wins count when stats exist")
        void getCurrentProgress_WhenStatsExist_ReturnsWins() {
            BattleParticipantRepository.BattleStatsProjection stats = mock(BattleParticipantRepository.BattleStatsProjection.class);
            when(stats.getWins()).thenReturn(10L);
            when(battleParticipantRepository.getStats(USER_ID)).thenReturn(stats);

            long progress = evaluator.getCurrentProgress(USER_ID);

            assertThat(progress).isEqualTo(10L);
            verify(battleParticipantRepository).getStats(USER_ID);
        }

        @Test
        @DisplayName("Returns 0 when stats are null")
        void getCurrentProgress_WhenStatsNull_ReturnsZero() {
            when(battleParticipantRepository.getStats(USER_ID)).thenReturn(null);

            long progress = evaluator.getCurrentProgress(USER_ID);

            assertThat(progress).isZero();
            verify(battleParticipantRepository).getStats(USER_ID);
        }
    }

    @Nested
    @DisplayName("isEligible")
    class IsEligibleTests {

        @Test
        @DisplayName("Returns true when progress equals or exceeds condition value")
        void isEligible_WhenProgressMeetsOrExceedsCondition_ReturnsTrue() {
            Badge badge = Badge.builder()
                    .conditionValue(5)
                    .build();

            assertThat(evaluator.isEligible(5L, badge)).isTrue();
            assertThat(evaluator.isEligible(8L, badge)).isTrue();
        }

        @Test
        @DisplayName("Returns false when progress is strictly below condition value")
        void isEligible_WhenProgressBelowCondition_ReturnsFalse() {
            Badge badge = Badge.builder()
                    .conditionValue(5)
                    .build();

            assertThat(evaluator.isEligible(4L, badge)).isFalse();
            assertThat(evaluator.isEligible(0L, badge)).isFalse();
        }
    }
}
