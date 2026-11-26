package com.elingo.admin.stats.service.impl;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.UserStatsResponses;
import com.elingo.admin.stats.service.UserStatsService;
import com.elingo.admin.stats.util.StatsZeroFiller;
import com.elingo.common.service.RedisService;
import com.elingo.gamification.dto.projection.DateCountProjection;
import com.elingo.gamification.dto.projection.LevelDistribution;
import com.elingo.gamification.service.GamificationStatsQueryService;
import com.elingo.premium.service.SubscriptionService;
import com.elingo.user.dto.projection.NewUserStats;
import com.elingo.user.service.UserService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "USER-STATS-SERVICE")
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S1192") // Giữ nguyên message log Redis chuẩn thống nhất giữa các hàm stats
public class UserStatsServiceImpl implements UserStatsService {

    private final UserService userService;
    private final GamificationStatsQueryService gamificationStatsQueryService;
    private final SubscriptionService subscriptionService;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    private static final long CACHE_TTL_GROWTH_MINUTES = 3L;
    private static final long CACHE_TTL_ACTIVITY_MINUTES = 3L;
    private static final long CACHE_TTL_BREAKDOWN_MINUTES = 5L;
    @Override
    public UserStatsResponses.UserGrowthResponse getUserGrowth(StatsDateRangeRequest request) {
        StatsDateRangeRequest safeRequest = sanitizeRequest(request);
        String cacheKey = String.format("admin:stats:users:growth:from=%s&to=%s&granularity=%s",
                safeRequest.from(), safeRequest.to(), safeRequest.granularity());

        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, UserStatsResponses.UserGrowthResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize JSON from Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }

        LocalDateTime fromTime = safeRequest.from().atStartOfDay();
        LocalDateTime toTime = safeRequest.to().plusDays(1).atStartOfDay();

        long totalBeforeRange = userService.countUsersBefore(fromTime);

        List<NewUserStats> rawData = userService.countNewUsers(
                fromTime, toTime, safeRequest.granularity());

        Map<String, Long> rawMap = new HashMap<>();
        for (NewUserStats proj : rawData) {
            if (proj.getPeriodKey() != null) {
                rawMap.put(proj.getPeriodKey(), proj.getCountVal() != null ? proj.getCountVal() : 0L);
            }
        }

        StatsZeroFiller.UserGrowthSeriesResult seriesResult = StatsZeroFiller.fillUserGrowthSeries(
                safeRequest.from(), safeRequest.to(), safeRequest.granularity(), rawMap, totalBeforeRange);

        UserStatsResponses.UserGrowthResponse response = new UserStatsResponses.UserGrowthResponse(
                totalBeforeRange,
                seriesResult.newUsers(),
                seriesResult.cumulativeUsers(),
                LocalDateTime.now()
        );

        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL_GROWTH_MINUTES);
        } catch (JacksonException e) {
            log.error("Failed to serialize JSON to Redis for key {}: {}", cacheKey, e.getMessage());
        }

        return response;
    }

    @Override
    public UserStatsResponses.UserActivityResponse getUserActivity(StatsDateRangeRequest request) {
        StatsDateRangeRequest safeRequest = sanitizeRequest(request);
        String cacheKey = String.format("admin:stats:users:activity:from=%s&to=%s&granularity=%s",
                safeRequest.from(), safeRequest.to(), safeRequest.granularity());

        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, UserStatsResponses.UserActivityResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize JSON from Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }

        LocalDateTime fromTime = safeRequest.from().atStartOfDay();
        LocalDateTime toTime = safeRequest.to().plusDays(1).atStartOfDay();

        List<DateCountProjection> rawData = gamificationStatsQueryService.countDauTimeSeries(fromTime, toTime);
        Map<String, Long> rawMap = new HashMap<>();
        for (DateCountProjection proj : rawData) {
            if (proj.getPeriodKey() != null) {
                rawMap.put(proj.getPeriodKey(), proj.getCountVal() != null ? proj.getCountVal() : 0L);
            }
        }

        List<UserStatsResponses.StatPoint<String, Long>> dauSeries = StatsZeroFiller.fillDauSeries(
                safeRequest.from(), safeRequest.to(), rawMap);

        Long currentDau = dauSeries.isEmpty() ? 0L : dauSeries.get(dauSeries.size() - 1).value();

        // WAU: 7 days up to toTime
        LocalDateTime wauSince = safeRequest.to().minusDays(6).atStartOfDay();
        long currentWau = gamificationStatsQueryService.countDistinctActiveUsers(wauSince, toTime);

        // MAU: 30 days up to toTime
        LocalDateTime mauSince = safeRequest.to().minusDays(29).atStartOfDay();
        long currentMau = gamificationStatsQueryService.countDistinctActiveUsers(mauSince, toTime);

        double stickinessPercentage = currentMau > 0
                ? Math.round(((double) currentDau / currentMau) * 10000.0) / 100.0
                : 0.0;

        UserStatsResponses.UserActivityResponse response = new UserStatsResponses.UserActivityResponse(
                currentDau,
                currentWau,
                currentMau,
                stickinessPercentage,
                dauSeries,
                LocalDateTime.now()
        );

        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL_ACTIVITY_MINUTES);
        } catch (JacksonException e) {
            log.error("Failed to serialize JSON to Redis for key {}: {}", cacheKey, e.getMessage());
        }

        return response;
    }

    @Override
    public UserStatsResponses.UserBreakdownResponse getUserBreakdown() {
        String cacheKey = "admin:stats:users:breakdown:latest";

        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, UserStatsResponses.UserBreakdownResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize JSON from Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }

        long totalUsers = userService.countTotalUsers();
        long verifiedCount = userService.countVerifiedUsers();
        long activeCount = userService.countActiveUsers();

        LocalDateTime now = LocalDateTime.now();
        long premiumCount = subscriptionService.countDistinctActivePremiumUsers(now);

        double verifiedPercentage = totalUsers > 0 ? Math.round(((double) verifiedCount / totalUsers) * 10000.0) / 100.0 : 0.0;
        double activePercentage = totalUsers > 0 ? Math.round(((double) activeCount / totalUsers) * 10000.0) / 100.0 : 0.0;
        double premiumPercentage = totalUsers > 0 ? Math.round(((double) premiumCount / totalUsers) * 10000.0) / 100.0 : 0.0;

        List<LevelDistribution> rawLevels = gamificationStatsQueryService.countUsersByLevel();
        List<UserStatsResponses.StatPoint<Short, Long>> levelDistribution = rawLevels.stream().map(lvl ->
                new UserStatsResponses.StatPoint<>(
                        lvl.getLevelNo(),
                        lvl.getUserCount() != null ? lvl.getUserCount() : 0L
                )
        ).toList();

        UserStatsResponses.UserBreakdownResponse response = new UserStatsResponses.UserBreakdownResponse(
                totalUsers,
                verifiedCount,
                verifiedPercentage,
                activeCount,
                activePercentage,
                premiumCount,
                premiumPercentage,
                levelDistribution,
                LocalDateTime.now()
        );

        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL_BREAKDOWN_MINUTES);
        } catch (JacksonException e) {
            log.error("Failed to serialize JSON to Redis for key {}: {}", cacheKey, e.getMessage());
        }

        return response;
    }

    private StatsDateRangeRequest sanitizeRequest(StatsDateRangeRequest request) {
        if (request == null) {
            return new StatsDateRangeRequest(null, null, null);
        }
        return request;
    }
}
