package com.elingo.admin.stats.service.impl;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.RevenueStatsResponses;
import com.elingo.admin.stats.service.RevenueStatsService;
import com.elingo.admin.stats.util.StatsZeroFiller;
import com.elingo.common.service.RedisService;
import com.elingo.premium.dto.projection.DateAmountCountProjection;
import com.elingo.premium.dto.projection.PlanRevenueStat;
import com.elingo.premium.dto.projection.PlanSubscriptionStat;
import com.elingo.premium.dto.projection.TransactionStatusCount;
import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.service.RevenueStatsQueryService;
import com.elingo.premium.service.SubscriptionService;
import com.elingo.user.service.UserService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "REVENUE-STATS-SERVICE")
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S1192") // Giữ nguyên message log Redis chuẩn thống nhất giữa các hàm stats
public class RevenueStatsServiceImpl implements RevenueStatsService {

    private final RevenueStatsQueryService revenueStatsQueryService;
    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    private static final long CACHE_TTL_TIMESERIES_MINUTES = 3L;
    private static final long CACHE_TTL_BREAKDOWN_MINUTES = 5L;
    private static final long CACHE_TTL_SUBSCRIPTIONS_MINUTES = 5L;

    @Override
    public RevenueStatsResponses.RevenueTimeSeriesResponse getRevenueTimeSeries(StatsDateRangeRequest request) {
        StatsDateRangeRequest safeRequest = sanitizeRequest(request);
        String cacheKey = String.format("admin:stats:revenue:timeseries:from=%s&to=%s&granularity=%s",
                safeRequest.from(), safeRequest.to(), safeRequest.granularity());

        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, RevenueStatsResponses.RevenueTimeSeriesResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize JSON from Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }

        LocalDateTime fromTime = safeRequest.from().atStartOfDay();
        LocalDateTime toTime = safeRequest.to().plusDays(1).atStartOfDay();

        List<DateAmountCountProjection> rawData = revenueStatsQueryService.getRevenueTimeSeries(
                fromTime, toTime, safeRequest.granularity());

        Map<String, StatsZeroFiller.RawRevenueData> rawMap = new HashMap<>();
        for (DateAmountCountProjection proj : rawData) {
            if (proj.getPeriodKey() != null) {
                rawMap.put(proj.getPeriodKey(), new StatsZeroFiller.RawRevenueData(
                        proj.getTotalAmount() != null ? proj.getTotalAmount() : BigDecimal.ZERO,
                        proj.getTxCount() != null ? proj.getTxCount() : 0L));
            }
        }

        List<RevenueStatsResponses.RevenuePoint> points = StatsZeroFiller.fillRevenueSeries(
                safeRequest.from(), safeRequest.to(), safeRequest.granularity(), rawMap);

        BigDecimal totalRevenue = points.stream()
                .map(RevenueStatsResponses.RevenuePoint::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalTx = points.stream()
                .mapToLong(RevenueStatsResponses.RevenuePoint::transactionCount)
                .sum();

        RevenueStatsResponses.RevenueTimeSeriesResponse response = new RevenueStatsResponses.RevenueTimeSeriesResponse(
                totalRevenue,
                totalTx,
                "VND",
                points,
                LocalDateTime.now()
        );

        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL_TIMESERIES_MINUTES);
        } catch (JacksonException e) {
            log.error("Failed to serialize JSON to Redis for key {}: {}", cacheKey, e.getMessage());
        }

        return response;
    }

    @Override
    @SuppressWarnings("java:S3776")
    public RevenueStatsResponses.RevenueBreakdownResponse getRevenueBreakdown(StatsDateRangeRequest request) {
        StatsDateRangeRequest safeRequest = sanitizeRequest(request);
        String cacheKey = String.format("admin:stats:revenue:breakdown:from=%s&to=%s",
                safeRequest.from(), safeRequest.to());

        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, RevenueStatsResponses.RevenueBreakdownResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize JSON from Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }

        LocalDateTime fromTime = safeRequest.from().atStartOfDay();
        LocalDateTime toTime = safeRequest.to().plusDays(1).atStartOfDay();

        List<PlanRevenueStat> planRaw = revenueStatsQueryService.getRevenueBreakdownByPlan(fromTime, toTime);
        BigDecimal totalPlanRevenue = planRaw.stream()
                .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RevenueStatsResponses.PlanRevenue> byPlan = planRaw.stream().map(p -> {
            BigDecimal amt = p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO;
            double pct = totalPlanRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? amt.multiply(BigDecimal.valueOf(100)).divide(totalPlanRevenue, 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            return new RevenueStatsResponses.PlanRevenue(
                    p.getPlanId(),
                    p.getPlanName(),
                    amt,
                    p.getTxCount() != null ? p.getTxCount() : 0L,
                    pct
            );
        }).toList();

        List<TransactionStatusCount> statusRaw = revenueStatsQueryService.countTransactionsByStatus(fromTime, toTime);
        long successCount = 0L;
        long pendingCount = 0L;
        long failedCount = 0L;
        long refundedCount = 0L;

        for (TransactionStatusCount item : statusRaw) {
            long count = item.getCountVal() != null ? item.getCountVal() : 0L;
            if (item.getStatus() == PaymentTransactionStatus.SUCCESS) {
                successCount += count;
            } else if (item.getStatus() == PaymentTransactionStatus.PENDING) {
                pendingCount += count;
            } else if (item.getStatus() == PaymentTransactionStatus.FAILED) {
                failedCount += count;
            } else if (item.getStatus() == PaymentTransactionStatus.REFUNDED) {
                refundedCount += count;
            }
        }
        long totalTransactions = successCount + pendingCount + failedCount + refundedCount;
        double successRate = totalTransactions > 0
                ? Math.round(((double) successCount / totalTransactions) * 10000.0) / 100.0
                : 0.0;

        RevenueStatsResponses.TransactionStatusBreakdown statusBreakdown = new RevenueStatsResponses.TransactionStatusBreakdown(
                totalTransactions,
                successCount,
                pendingCount,
                failedCount,
                refundedCount,
                successRate
        );

        RevenueStatsResponses.RevenueBreakdownResponse response = new RevenueStatsResponses.RevenueBreakdownResponse(
                byPlan,
                statusBreakdown,
                LocalDateTime.now()
        );

        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL_BREAKDOWN_MINUTES);
        } catch (JacksonException e) {
            log.error("Failed to serialize JSON to Redis for key {}: {}", cacheKey, e.getMessage());
        }

        return response;
    }

    @Override
    public RevenueStatsResponses.SubscriptionStatsResponse getSubscriptionStats() {
        String cacheKey = "admin:stats:subscriptions:latest";

        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, RevenueStatsResponses.SubscriptionStatsResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize JSON from Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<PlanSubscriptionStat> planRaw = subscriptionService.countActiveSubscriptionsByPlan(now);
        long totalActiveSubs = subscriptionService.countTotalActiveSubscriptions(now);
        long totalPremiumUsers = subscriptionService.countDistinctActivePremiumUsers(now);
        long totalSystemUsers = userService.countTotalUsers();

        double premiumRate = totalSystemUsers > 0
                ? Math.round(((double) totalPremiumUsers / totalSystemUsers) * 10000.0) / 100.0
                : 0.0;

        List<RevenueStatsResponses.PlanSubscription> activeByPlan = planRaw.stream().map(p -> {
            long count = p.getActiveCount() != null ? p.getActiveCount() : 0L;
            double pct = totalActiveSubs > 0
                    ? Math.round(((double) count / totalActiveSubs) * 10000.0) / 100.0
                    : 0.0;
            return new RevenueStatsResponses.PlanSubscription(
                    p.getPlanId(),
                    p.getPlanName(),
                    count,
                    pct
            );
        }).toList();

        RevenueStatsResponses.SubscriptionStatsResponse response = new RevenueStatsResponses.SubscriptionStatsResponse(
                totalActiveSubs,
                totalPremiumUsers,
                totalSystemUsers,
                premiumRate,
                activeByPlan,
                LocalDateTime.now()
        );

        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL_SUBSCRIPTIONS_MINUTES);
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
