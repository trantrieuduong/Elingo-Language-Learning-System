package com.elingo.gamification.listener;

import com.elingo.gamification.entity.LeaderboardPeriodType;
import com.elingo.gamification.service.LeaderboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LeaderboardWarmupListenerTest {

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private LeaderboardWarmupListener warmupListener;

    @Nested
    @DisplayName("warmupLeaderboards")
    class WarmupLeaderboardsTests {

        @Test
        @DisplayName("Should warm up both WEEKLY and MONTHLY leaderboards successfully on application ready")
        void warmupLeaderboards_Success() {
            warmupListener.warmupLeaderboards();

            verify(leaderboardService).warmupLeaderboardIfAbsent(LeaderboardPeriodType.WEEKLY);
            verify(leaderboardService).warmupLeaderboardIfAbsent(LeaderboardPeriodType.MONTHLY);
        }

        @Test
        @DisplayName("Should catch and handle exception gracefully when service fails during warmup")
        void warmupLeaderboards_WhenServiceThrows_HandlesGracefully() {
            doThrow(new RuntimeException("Redis connection error"))
                    .when(leaderboardService).warmupLeaderboardIfAbsent(LeaderboardPeriodType.WEEKLY);

            assertDoesNotThrow(() -> warmupListener.warmupLeaderboards());

            verify(leaderboardService).warmupLeaderboardIfAbsent(LeaderboardPeriodType.WEEKLY);
            verify(leaderboardService, never()).warmupLeaderboardIfAbsent(LeaderboardPeriodType.MONTHLY);
        }
    }
}
