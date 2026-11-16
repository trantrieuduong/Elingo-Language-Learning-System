package com.elingo.gamification.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.service.RedisService;
import com.elingo.gamification.dto.response.LeaderboardItemResponse;
import com.elingo.gamification.dto.response.LeaderboardResponse;
import com.elingo.gamification.dto.response.MyLeaderboardRankResponse;
import com.elingo.gamification.entity.LeaderboardPeriodType;
import com.elingo.gamification.entity.LeaderboardSnapshot;
import com.elingo.gamification.repository.LeaderboardSnapshotRepository;
import com.elingo.gamification.repository.XpEventRepository;
import com.elingo.gamification.service.impl.LeaderboardServiceImpl;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LeaderboardServiceTest {

    @Mock
    private RedisService redis;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaderboardSnapshotRepository snapshotRepository;

    @Mock
    private XpEventRepository xpEventRepository;

    @Mock
    private LeaderboardServiceImpl self;

    @InjectMocks
    private LeaderboardServiceImpl leaderboardService;

    private static final Long USER_1 = 10L;
    private static final Long USER_2 = 20L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(leaderboardService, "self", self);
    }

    @Nested
    @DisplayName("recordXpForLeaderboard")
    class RecordXpForLeaderboardTests {

        @Test
        @DisplayName("Should increment XP in both weekly and monthly sorted sets and set TTL")
        void recordXp_Success() {
            leaderboardService.recordXpForLeaderboard(USER_1, 50);

            verify(redis, times(2)).zIncrBy(anyString(), eq(String.valueOf(USER_1)), eq(50.0));
            verify(redis).expire(contains("weekly"), eq(Duration.ofDays(14)));
            verify(redis).expire(contains("monthly"), eq(Duration.ofDays(60)));
        }

        @Test
        @DisplayName("Should skip recording when XP amount is zero or negative")
        void recordXp_WhenAmountIsZeroOrNegative_ShouldSkip() {
            leaderboardService.recordXpForLeaderboard(USER_1, 0);
            leaderboardService.recordXpForLeaderboard(USER_1, -10);

            verify(redis, never()).zIncrBy(anyString(), anyString(), anyDouble());
        }

        @Test
        @DisplayName("Should skip recording when userId is null")
        void recordXp_WhenUserIdIsNull_ShouldSkip() {
            leaderboardService.recordXpForLeaderboard(null, 100);

            verify(redis, never()).zIncrBy(anyString(), anyString(), anyDouble());
        }

        @Test
        @DisplayName("Should catch and handle Redis exception gracefully without throwing")
        void recordXp_WhenRedisFails_ShouldHandleGracefully() {
            doThrow(new RuntimeException("Redis connection error"))
                    .when(redis).zIncrBy(anyString(), anyString(), anyDouble());

            assertDoesNotThrow(() -> leaderboardService.recordXpForLeaderboard(USER_1, 50));
        }
    }

    @Nested
    @DisplayName("getRealtimeLeaderboard")
    class GetRealtimeLeaderboardTests {

        @Test
        @DisplayName("Should return paginated leaderboard and current user rank successfully")
        void getRealtimeLeaderboard_Success() {
            Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
            tuples.add(new DefaultTypedTuple<>(String.valueOf(USER_1), 100.0));
            tuples.add(new DefaultTypedTuple<>(String.valueOf(USER_2), 80.0));

            User user1 = User.builder().id(USER_1).username("player1").build();
            User user2 = User.builder().id(USER_2).username("player2").build();

            when(redis.exists(anyString())).thenReturn(true);
            when(redis.zCard(anyString())).thenReturn(2L);
            when(redis.zReverseRangeWithScores(anyString(), eq(0L), eq(19L))).thenReturn(tuples);
            when(userRepository.findAllById(List.of(USER_1, USER_2))).thenReturn(List.of(user1, user2));
            when(redis.zReverseRank(anyString(), eq(String.valueOf(USER_1)))).thenReturn(0L);
            when(redis.zScore(anyString(), eq(String.valueOf(USER_1)))).thenReturn(100.0);

            LeaderboardResponse response = leaderboardService.getRealtimeLeaderboard(
                    USER_1,
                    LeaderboardPeriodType.WEEKLY,
                    PageRequest.of(0, 20)
            );

            assertThat(response.periodType()).isEqualTo(LeaderboardPeriodType.WEEKLY);
            assertThat(response.leaderboard()).isNotNull();
            assertThat(response.leaderboard().totalElements()).isEqualTo(2L);
            assertThat(response.leaderboard().content()).hasSize(2);
            assertThat(response.leaderboard().content().get(0).username()).isEqualTo("player1");
            assertThat(response.leaderboard().content().get(0).totalXp()).isEqualTo(100);
            assertThat(response.leaderboard().content().get(0).rank()).isEqualTo(1);

            assertThat(response.myRank()).isNotNull();
            assertThat(response.myRank().rank()).isEqualTo(1);
            assertThat(response.myRank().totalXp()).isEqualTo(100);
            verify(self, never()).syncLeaderboardFromDatabase(any());
        }

        @Test
        @DisplayName("Should trigger sync from database when Redis key is absent")
        void getRealtimeLeaderboard_WhenKeyMissing_TriggersDatabaseSync() {
            when(redis.exists(anyString())).thenReturn(false);
            when(redis.zCard(anyString())).thenReturn(0L);
            when(redis.zReverseRangeWithScores(anyString(), eq(0L), eq(19L))).thenReturn(Set.of());

            LeaderboardResponse response = leaderboardService.getRealtimeLeaderboard(
                    null,
                    LeaderboardPeriodType.WEEKLY,
                    PageRequest.of(0, 20)
            );

            assertThat(response.leaderboard().content()).isEmpty();
            assertThat(response.leaderboard().totalElements()).isZero();
            assertThat(response.myRank()).isNull();
            verify(self).syncLeaderboardFromDatabase(LeaderboardPeriodType.WEEKLY);
        }

        @Test
        @DisplayName("Should return null myRank when currentUserId is null")
        void getRealtimeLeaderboard_WhenCurrentUserIdIsNull_ReturnsNullMyRank() {
            when(redis.exists(anyString())).thenReturn(true);
            when(redis.zCard(anyString())).thenReturn(0L);
            when(redis.zReverseRangeWithScores(anyString(), eq(0L), eq(9L))).thenReturn(Set.of());

            LeaderboardResponse response = leaderboardService.getRealtimeLeaderboard(
                    null,
                    LeaderboardPeriodType.MONTHLY,
                    PageRequest.of(0, 10)
            );

            assertThat(response.myRank()).isNull();
            assertThat(response.periodType()).isEqualTo(LeaderboardPeriodType.MONTHLY);
        }
    }

    @Nested
    @DisplayName("getMyRank")
    class GetMyRankTests {

        @Test
        @DisplayName("Should return null rank and zero stats when user is not present in leaderboard")
        void getMyRank_WhenUserUnranked_ReturnsNullRank() {
            when(redis.zReverseRank(anyString(), eq(String.valueOf(USER_1)))).thenReturn(null);

            MyLeaderboardRankResponse response = leaderboardService.getMyRank(USER_1, LeaderboardPeriodType.WEEKLY);

            assertThat(response.rank()).isNull();
            assertThat(response.totalXp()).isZero();
            assertThat(response.diffToNextRank()).isZero();
            assertThat(response.periodType()).isEqualTo(LeaderboardPeriodType.WEEKLY);
        }

        @Test
        @DisplayName("Should calculate diff to next rank when user is ranked below #1")
        void getMyRank_WhenRankedBelowTop_CalculatesDiffToNextRank() {
            when(redis.zReverseRank(anyString(), eq(String.valueOf(USER_2)))).thenReturn(1L); // rank 2
            when(redis.zScore(anyString(), eq(String.valueOf(USER_2)))).thenReturn(80.0);

            Set<ZSetOperations.TypedTuple<String>> aboveTuples = Set.of(
                    new DefaultTypedTuple<>(String.valueOf(USER_1), 100.0)
            );
            when(redis.zReverseRangeWithScores(anyString(), eq(0L), eq(0L))).thenReturn(aboveTuples);

            MyLeaderboardRankResponse response = leaderboardService.getMyRank(USER_2, LeaderboardPeriodType.WEEKLY);

            assertThat(response.rank()).isEqualTo(2);
            assertThat(response.totalXp()).isEqualTo(80);
            assertThat(response.diffToNextRank()).isEqualTo(21); // (100 - 80) + 1 = 21
        }

        @Test
        @DisplayName("Should have zero diff to next rank when user is #1")
        void getMyRank_WhenUserIsTopRank_DiffToNextIsZero() {
            when(redis.zReverseRank(anyString(), eq(String.valueOf(USER_1)))).thenReturn(0L); // rank 1
            when(redis.zScore(anyString(), eq(String.valueOf(USER_1)))).thenReturn(150.0);

            MyLeaderboardRankResponse response = leaderboardService.getMyRank(USER_1, LeaderboardPeriodType.MONTHLY);

            assertThat(response.rank()).isEqualTo(1);
            assertThat(response.totalXp()).isEqualTo(150);
            assertThat(response.diffToNextRank()).isZero();
        }
    }

    @Nested
    @DisplayName("syncLeaderboardFromDatabase")
    class SyncLeaderboardFromDatabaseTests {

        @Test
        @DisplayName("Should synchronize weekly XP aggregates from database to Redis sorted set")
        void syncLeaderboardFromDatabase_Weekly_Success() {
            XpEventRepository.UserPeriodXp record1 = mock(XpEventRepository.UserPeriodXp.class);
            when(record1.getUserId()).thenReturn(USER_1);
            when(record1.getTotalXp()).thenReturn(150L);

            when(xpEventRepository.sumXpGroupByUserSince(any(LocalDateTime.class)))
                    .thenReturn(List.of(record1));

            leaderboardService.syncLeaderboardFromDatabase(LeaderboardPeriodType.WEEKLY);

            verify(redis).zAdd(contains("weekly"), eq(String.valueOf(USER_1)), eq(150.0));
            verify(redis).expire(contains("weekly"), eq(Duration.ofDays(14)));
        }

        @Test
        @DisplayName("Should synchronize monthly XP aggregates from database to Redis sorted set")
        void syncLeaderboardFromDatabase_Monthly_Success() {
            XpEventRepository.UserPeriodXp record1 = mock(XpEventRepository.UserPeriodXp.class);
            when(record1.getUserId()).thenReturn(USER_2);
            when(record1.getTotalXp()).thenReturn(500L);

            when(xpEventRepository.sumXpGroupByUserSince(any(LocalDateTime.class)))
                    .thenReturn(List.of(record1));

            leaderboardService.syncLeaderboardFromDatabase(LeaderboardPeriodType.MONTHLY);

            verify(redis).zAdd(contains("monthly"), eq(String.valueOf(USER_2)), eq(500.0));
            verify(redis).expire(contains("monthly"), eq(Duration.ofDays(60)));
        }

        @Test
        @DisplayName("Should log and complete normally when no XP records exist in database")
        void syncLeaderboardFromDatabase_WhenEmpty_DoesNotWriteToRedis() {
            when(xpEventRepository.sumXpGroupByUserSince(any(LocalDateTime.class)))
                    .thenReturn(List.of());

            leaderboardService.syncLeaderboardFromDatabase(LeaderboardPeriodType.WEEKLY);

            verify(redis, never()).zAdd(anyString(), anyString(), anyDouble());
        }

        @Test
        @DisplayName("Should handle database exception gracefully without throwing")
        void syncLeaderboardFromDatabase_WhenExceptionThrown_HandlesGracefully() {
            when(xpEventRepository.sumXpGroupByUserSince(any(LocalDateTime.class)))
                    .thenThrow(new RuntimeException("Database query error"));

            assertDoesNotThrow(() -> leaderboardService.syncLeaderboardFromDatabase(LeaderboardPeriodType.WEEKLY));
        }
    }

    @Nested
    @DisplayName("warmupLeaderboardIfAbsent")
    class WarmupLeaderboardIfAbsentTests {

        @Test
        @DisplayName("Should skip sync when Redis key already exists")
        void warmupLeaderboardIfAbsent_WhenKeyExists_SkipsSync() {
            when(redis.exists(anyString())).thenReturn(true);

            leaderboardService.warmupLeaderboardIfAbsent(LeaderboardPeriodType.WEEKLY);

            verify(self, never()).syncLeaderboardFromDatabase(any());
        }

        @Test
        @DisplayName("Should trigger sync when Redis key is missing")
        void warmupLeaderboardIfAbsent_WhenKeyMissing_CallsSync() {
            when(redis.exists(anyString())).thenReturn(false);

            leaderboardService.warmupLeaderboardIfAbsent(LeaderboardPeriodType.WEEKLY);

            verify(self).syncLeaderboardFromDatabase(LeaderboardPeriodType.WEEKLY);
        }

        @Test
        @DisplayName("Should catch and handle exception gracefully when Redis check throws")
        void warmupLeaderboardIfAbsent_WhenRedisThrows_HandlesGracefully() {
            when(redis.exists(anyString())).thenThrow(new RuntimeException("Redis unavailable"));

            assertDoesNotThrow(() -> leaderboardService.warmupLeaderboardIfAbsent(LeaderboardPeriodType.MONTHLY));
        }
    }

    @Nested
    @DisplayName("getHistoricalLeaderboard")
    class GetHistoricalLeaderboardTests {

        @Test
        @DisplayName("Should retrieve historical leaderboard snapshot and map to standard PageResponse")
        void getHistoricalLeaderboard_Success() {
            User user = User.builder().id(USER_1).username("player1").avatarUrl(null).build();
            LeaderboardSnapshot snapshot = LeaderboardSnapshot.builder()
                    .user(user)
                    .rank(1)
                    .totalXp(300)
                    .periodType(LeaderboardPeriodType.WEEKLY)
                    .periodKey("2026-W45")
                    .build();

            Pageable pageable = PageRequest.of(0, 20);
            when(snapshotRepository.findByPeriod(eq(LeaderboardPeriodType.WEEKLY), eq("2026-W45"), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(snapshot), pageable, 1));

            PageResponse<LeaderboardItemResponse> result = leaderboardService.getHistoricalLeaderboard(
                    LeaderboardPeriodType.WEEKLY,
                    "2026-W45",
                    pageable
            );

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().username()).isEqualTo("player1");
            assertThat(result.totalElements()).isEqualTo(1L);
            assertThat(result.page()).isEqualTo(1); // PageResponse is 1-based
        }
    }

    @Nested
    @DisplayName("snapshotLeaderboard")
    class SnapshotLeaderboardTests {

        @Test
        @DisplayName("Should skip snapshot creation if snapshot already exists for period")
        void snapshotLeaderboard_WhenAlreadyExists_Skips() {
            when(snapshotRepository.existsByPeriodTypeAndPeriodKey(LeaderboardPeriodType.WEEKLY, "2026-W46"))
                    .thenReturn(true);

            leaderboardService.snapshotLeaderboard(LeaderboardPeriodType.WEEKLY, "2026-W46");

            verify(redis, never()).zReverseRangeWithScores(anyString(), anyLong(), anyLong());
            verify(snapshotRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should skip snapshot creation if Redis leaderboard is empty")
        void snapshotLeaderboard_WhenRedisEmpty_Skips() {
            when(snapshotRepository.existsByPeriodTypeAndPeriodKey(LeaderboardPeriodType.WEEKLY, "2026-W46"))
                    .thenReturn(false);
            when(redis.zReverseRangeWithScores(anyString(), eq(0L), eq(499L)))
                    .thenReturn(Set.of());

            leaderboardService.snapshotLeaderboard(LeaderboardPeriodType.WEEKLY, "2026-W46");

            verify(snapshotRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should persist top entries to snapshot repository when snapshot does not exist")
        void snapshotLeaderboard_Success() {
            when(snapshotRepository.existsByPeriodTypeAndPeriodKey(LeaderboardPeriodType.WEEKLY, "2026-W46"))
                    .thenReturn(false);

            Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
            tuples.add(new DefaultTypedTuple<>(String.valueOf(USER_1), 350.0));
            when(redis.zReverseRangeWithScores(anyString(), eq(0L), eq(499L)))
                    .thenReturn(tuples);

            User user = User.builder().id(USER_1).username("player1").build();
            when(userRepository.findAllById(List.of(USER_1))).thenReturn(List.of(user));

            leaderboardService.snapshotLeaderboard(LeaderboardPeriodType.WEEKLY, "2026-W46");

            verify(snapshotRepository).saveAll(argThat(iterable -> {
                List<LeaderboardSnapshot> list = (List<LeaderboardSnapshot>) iterable;
                return list.size() == 1
                        && list.getFirst().getRank() == 1
                        && list.getFirst().getTotalXp() == 350
                        && list.getFirst().getPeriodKey().equals("2026-W46");
            }));
        }
    }
}
