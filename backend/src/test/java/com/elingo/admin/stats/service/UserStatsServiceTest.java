package com.elingo.admin.stats.service;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.UserStatsResponses;
import com.elingo.admin.stats.service.impl.UserStatsServiceImpl;
import com.elingo.common.enums.StatsGranularity;
import com.elingo.common.service.RedisService;
import com.elingo.gamification.dto.projection.LevelDistribution;
import com.elingo.gamification.dto.projection.DateCountProjection;
import com.elingo.gamification.service.GamificationStatsQueryService;
import com.elingo.premium.service.SubscriptionService;
import com.elingo.user.dto.projection.NewUserStats;
import com.elingo.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserStatsServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private GamificationStatsQueryService gamificationStatsQueryService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private RedisService redisService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserStatsServiceImpl userStatsService;

    private final LocalDate fromDate = LocalDate.of(2026, 11, 1);
    private final LocalDate toDate = LocalDate.of(2026, 11, 3);
    private final StatsDateRangeRequest defaultRequest = StatsDateRangeRequest.of(fromDate, toDate, StatsGranularity.DAY);

    @Nested
    @DisplayName("getUserGrowth")
    class GetUserGrowthTests {

        @Test
        @DisplayName("Should return cached response when present in Redis")
        void getUserGrowth_CacheHit_ReturnsCached() throws Exception {
            String cacheKey = "admin:stats:users:growth:from=2026-11-01&to=2026-11-03&granularity=DAY";
            String jsonContent = "{\"totalUsersBeforeRange\": 100}";
            UserStatsResponses.UserGrowthResponse expected = new UserStatsResponses.UserGrowthResponse(
                    100L, List.of(), List.of(), LocalDateTime.now()
            );

            when(redisService.get(cacheKey)).thenReturn(jsonContent);
            when(objectMapper.readValue(jsonContent, UserStatsResponses.UserGrowthResponse.class)).thenReturn(expected);

            UserStatsResponses.UserGrowthResponse actual = userStatsService.getUserGrowth(defaultRequest);

            assertThat(actual).isEqualTo(expected);
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should fetch from UserService, apply zero-filling, and save to cache on cache miss")
        void getUserGrowth_CacheMiss_FetchesAndCaches() throws Exception {
            String cacheKey = "admin:stats:users:growth:from=2026-11-01&to=2026-11-03&granularity=DAY";
            when(redisService.get(cacheKey)).thenReturn(null);

            when(userService.countUsersBefore(any(LocalDateTime.class))).thenReturn(50L);

            NewUserStats projection = mock(NewUserStats.class);
            when(projection.getPeriodKey()).thenReturn("2026-11-01");
            when(projection.getCountVal()).thenReturn(10L);
            when(userService.countNewUsers(any(LocalDateTime.class), any(LocalDateTime.class), eq(StatsGranularity.DAY)))
                    .thenReturn(List.of(projection));

            when(objectMapper.writeValueAsString(any())).thenReturn("{\"cached\":true}");

            UserStatsResponses.UserGrowthResponse actual = userStatsService.getUserGrowth(defaultRequest);

            assertThat(actual.totalBeforeRange()).isEqualTo(50L);
            assertThat(actual.newUsers()).hasSize(3);
            assertThat(actual.cumulativeUsers()).hasSize(3);
            // Day 1 has 10 users, initial was 50 -> cumulative 60
            assertThat(actual.cumulativeUsers().get(0).value()).isEqualTo(60L);

            verify(redisService).save(eq(cacheKey), eq("{\"cached\":true}"), eq(3L));
        }

        @Test
        @DisplayName("Should fall back to database query when Redis cache read fails with JacksonException")
        void getUserGrowth_CacheReadError_FallsBackToDb() throws Exception {
            String cacheKey = "admin:stats:users:growth:from=2026-11-01&to=2026-11-03&granularity=DAY";
            when(redisService.get(cacheKey)).thenReturn("corrupted-json");
            when(objectMapper.readValue(eq("corrupted-json"), eq(UserStatsResponses.UserGrowthResponse.class)))
                    .thenThrow(mock(JacksonException.class));

            when(userService.countUsersBefore(any())).thenReturn(0L);
            when(userService.countNewUsers(any(), any(), any())).thenReturn(Collections.emptyList());

            UserStatsResponses.UserGrowthResponse actual = userStatsService.getUserGrowth(defaultRequest);

            assertThat(actual.totalBeforeRange()).isEqualTo(0L);
            verify(userService).countNewUsers(any(), any(), any());
        }

        @Test
        @DisplayName("Should return response even when Redis serialization fails")
        void getUserGrowth_CacheWriteError_ReturnsResponse() throws Exception {
            when(redisService.get(anyString())).thenReturn(null);
            when(userService.countUsersBefore(any())).thenReturn(10L);
            when(userService.countNewUsers(any(), any(), any())).thenReturn(Collections.emptyList());
            when(objectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));

            UserStatsResponses.UserGrowthResponse actual = userStatsService.getUserGrowth(defaultRequest);

            assertThat(actual).isNotNull();
            assertThat(actual.totalBeforeRange()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("getUserActivity")
    class GetUserActivityTests {

        @Test
        @DisplayName("Should return cached response on Redis cache hit")
        void getUserActivity_CacheHit_ReturnsCached() throws Exception {
            String cacheKey = "admin:stats:users:activity:from=2026-11-01&to=2026-11-03&granularity=DAY";
            UserStatsResponses.UserActivityResponse expected = new UserStatsResponses.UserActivityResponse(
                    10L, 50L, 200L, 5.0, List.of(), LocalDateTime.now()
            );

            when(redisService.get(cacheKey)).thenReturn("{\"dau\":10}");
            when(objectMapper.readValue("{\"dau\":10}", UserStatsResponses.UserActivityResponse.class)).thenReturn(expected);

            UserStatsResponses.UserActivityResponse actual = userStatsService.getUserActivity(defaultRequest);

            assertThat(actual).isEqualTo(expected);
            verifyNoInteractions(gamificationStatsQueryService);
        }

        @Test
        @DisplayName("Should calculate DAU series, WAU, MAU, and stickiness ratio on cache miss")
        void getUserActivity_CacheMiss_CalculatesMetrics() throws Exception {
            when(redisService.get(anyString())).thenReturn(null);

            DateCountProjection dauProj = mock(DateCountProjection.class);
            when(dauProj.getPeriodKey()).thenReturn("2026-11-03");
            when(dauProj.getCountVal()).thenReturn(25L);

            when(gamificationStatsQueryService.countDauTimeSeries(any(), any())).thenReturn(List.of(dauProj));
            when(gamificationStatsQueryService.countDistinctActiveUsers(any(), any()))
                    .thenReturn(70L)  // WAU
                    .thenReturn(100L); // MAU

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            UserStatsResponses.UserActivityResponse actual = userStatsService.getUserActivity(defaultRequest);

            // Last day (2026-11-03) has DAU = 25
            assertThat(actual.currentDau()).isEqualTo(25L);
            assertThat(actual.currentWau()).isEqualTo(70L);
            assertThat(actual.currentMau()).isEqualTo(100L);
            // Stickiness = (25 / 100) * 100 = 25.0%
            assertThat(actual.stickinessPercentage()).isEqualTo(25.0);
            assertThat(actual.dauTimeSeries()).hasSize(3);

            verify(redisService).save(anyString(), anyString(), eq(3L));
        }

        @Test
        @DisplayName("Should set stickiness percentage to 0.0 when MAU is zero to avoid division by zero")
        void getUserActivity_MauZero_StickinessIsZero() {
            when(redisService.get(anyString())).thenReturn(null);
            when(gamificationStatsQueryService.countDauTimeSeries(any(), any())).thenReturn(Collections.emptyList());
            when(gamificationStatsQueryService.countDistinctActiveUsers(any(), any())).thenReturn(0L);

            UserStatsResponses.UserActivityResponse actual = userStatsService.getUserActivity(defaultRequest);

            assertThat(actual.currentMau()).isEqualTo(0L);
            assertThat(actual.stickinessPercentage()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getUserBreakdown")
    class GetUserBreakdownTests {

        @Test
        @DisplayName("Should return cached response on Redis cache hit")
        void getUserBreakdown_CacheHit_ReturnsCached() throws Exception {
            String cacheKey = "admin:stats:users:breakdown:latest";
            UserStatsResponses.UserBreakdownResponse expected = new UserStatsResponses.UserBreakdownResponse(
                    100L, 80L, 80.0, 90L, 90.0, 10L, 10.0, List.of(), LocalDateTime.now()
            );

            when(redisService.get(cacheKey)).thenReturn("{\"totalUsers\":100}");
            when(objectMapper.readValue("{\"totalUsers\":100}", UserStatsResponses.UserBreakdownResponse.class)).thenReturn(expected);

            UserStatsResponses.UserBreakdownResponse actual = userStatsService.getUserBreakdown();

            assertThat(actual).isEqualTo(expected);
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should compute user ratios and level distribution on cache miss")
        void getUserBreakdown_CacheMiss_ComputesPercentages() throws Exception {
            when(redisService.get(anyString())).thenReturn(null);

            when(userService.countTotalUsers()).thenReturn(200L);
            when(userService.countVerifiedUsers()).thenReturn(150L);
            when(userService.countActiveUsers()).thenReturn(180L);
            when(subscriptionService.countDistinctActivePremiumUsers(any())).thenReturn(50L);

            LevelDistribution levelProj = mock(LevelDistribution.class);
            when(levelProj.getLevelNo()).thenReturn((short) 1);
            when(levelProj.getUserCount()).thenReturn(120L);
            when(gamificationStatsQueryService.countUsersByLevel()).thenReturn(List.of(levelProj));

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            UserStatsResponses.UserBreakdownResponse actual = userStatsService.getUserBreakdown();

            assertThat(actual.totalUsers()).isEqualTo(200L);
            assertThat(actual.verifiedCount()).isEqualTo(150L);
            assertThat(actual.verifiedPercentage()).isEqualTo(75.0);
            assertThat(actual.activeCount()).isEqualTo(180L);
            assertThat(actual.activePercentage()).isEqualTo(90.0);
            assertThat(actual.premiumCount()).isEqualTo(50L);
            assertThat(actual.premiumPercentage()).isEqualTo(25.0);
            assertThat(actual.levelDistribution()).hasSize(1);
            assertThat(actual.levelDistribution().get(0).key()).isEqualTo((short) 1);
            assertThat(actual.levelDistribution().get(0).value()).isEqualTo(120L);

            verify(redisService).save(eq("admin:stats:users:breakdown:latest"), anyString(), eq(5L));
        }

        @Test
        @DisplayName("Should return 0.0 percentages when total users is 0")
        void getUserBreakdown_ZeroTotalUsers_ReturnsZeroPercentages() {
            when(redisService.get(anyString())).thenReturn(null);
            when(userService.countTotalUsers()).thenReturn(0L);
            when(userService.countVerifiedUsers()).thenReturn(0L);
            when(userService.countActiveUsers()).thenReturn(0L);
            when(subscriptionService.countDistinctActivePremiumUsers(any())).thenReturn(0L);
            when(gamificationStatsQueryService.countUsersByLevel()).thenReturn(Collections.emptyList());

            UserStatsResponses.UserBreakdownResponse actual = userStatsService.getUserBreakdown();

            assertThat(actual.totalUsers()).isEqualTo(0L);
            assertThat(actual.verifiedPercentage()).isEqualTo(0.0);
            assertThat(actual.activePercentage()).isEqualTo(0.0);
            assertThat(actual.premiumPercentage()).isEqualTo(0.0);
        }
    }
}
