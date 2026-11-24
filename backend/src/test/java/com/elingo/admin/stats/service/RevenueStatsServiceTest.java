package com.elingo.admin.stats.service;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.RevenueStatsResponses;
import com.elingo.admin.stats.service.impl.RevenueStatsServiceImpl;
import com.elingo.common.enums.StatsGranularity;
import com.elingo.common.service.RedisService;
import com.elingo.premium.dto.projection.DateAmountCountProjection;
import com.elingo.premium.dto.projection.PlanRevenueStat;
import com.elingo.premium.dto.projection.PlanSubscriptionStat;
import com.elingo.premium.dto.projection.TransactionStatusCount;
import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.service.RevenueStatsQueryService;
import com.elingo.premium.service.SubscriptionService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RevenueStatsServiceTest {

    @Mock
    private RevenueStatsQueryService revenueStatsQueryService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UserService userService;

    @Mock
    private RedisService redisService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RevenueStatsServiceImpl revenueStatsService;

    private final LocalDate fromDate = LocalDate.of(2026, 11, 1);
    private final LocalDate toDate = LocalDate.of(2026, 11, 3);
    private final StatsDateRangeRequest defaultRequest = StatsDateRangeRequest.of(fromDate, toDate, StatsGranularity.DAY);

    @Nested
    @DisplayName("getRevenueTimeSeries")
    class GetRevenueTimeSeriesTests {

        @Test
        @DisplayName("Should return cached response on Redis cache hit")
        void getRevenueTimeSeries_CacheHit_ReturnsCached() throws Exception {
            String cacheKey = "admin:stats:revenue:timeseries:from=2026-11-01&to=2026-11-03&granularity=DAY";
            RevenueStatsResponses.RevenueTimeSeriesResponse expected = new RevenueStatsResponses.RevenueTimeSeriesResponse(
                    BigDecimal.valueOf(1_000_000), 10L, "VND", List.of(), LocalDateTime.now()
            );

            when(redisService.get(cacheKey)).thenReturn("{\"total\":1000000}");
            when(objectMapper.readValue("{\"total\":1000000}", RevenueStatsResponses.RevenueTimeSeriesResponse.class))
                    .thenReturn(expected);

            RevenueStatsResponses.RevenueTimeSeriesResponse actual = revenueStatsService.getRevenueTimeSeries(defaultRequest);

            assertThat(actual).isEqualTo(expected);
            verifyNoInteractions(revenueStatsQueryService);
        }

        @Test
        @DisplayName("Should fetch timeseries, fill zeros, calculate cumulative and cache on cache miss")
        void getRevenueTimeSeries_CacheMiss_FetchesAndCaches() throws Exception {
            when(redisService.get(anyString())).thenReturn(null);

            DateAmountCountProjection proj = mock(DateAmountCountProjection.class);
            when(proj.getPeriodKey()).thenReturn("2026-11-01");
            when(proj.getTotalAmount()).thenReturn(BigDecimal.valueOf(200_000));
            when(proj.getTxCount()).thenReturn(2L);

            when(revenueStatsQueryService.getRevenueTimeSeries(any(), any(), eq(StatsGranularity.DAY)))
                    .thenReturn(List.of(proj));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            RevenueStatsResponses.RevenueTimeSeriesResponse actual = revenueStatsService.getRevenueTimeSeries(defaultRequest);

            assertThat(actual.totalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(200_000));
            assertThat(actual.totalSuccessTransactions()).isEqualTo(2L);
            assertThat(actual.currency()).isEqualTo("VND");
            assertThat(actual.points()).hasSize(3);

            verify(redisService).save(anyString(), anyString(), eq(3L));
        }

        @Test
        @DisplayName("Should fall back to database query when Redis cache read fails with JacksonException")
        void getRevenueTimeSeries_CacheReadError_FallsBackToDb() throws Exception {
            when(redisService.get(anyString())).thenReturn("bad-json");
            when(objectMapper.readValue(eq("bad-json"), eq(RevenueStatsResponses.RevenueTimeSeriesResponse.class)))
                    .thenThrow(mock(JacksonException.class));
            when(revenueStatsQueryService.getRevenueTimeSeries(any(), any(), any())).thenReturn(Collections.emptyList());

            RevenueStatsResponses.RevenueTimeSeriesResponse actual = revenueStatsService.getRevenueTimeSeries(defaultRequest);

            assertThat(actual).isNotNull();
            verify(revenueStatsQueryService).getRevenueTimeSeries(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getRevenueBreakdown")
    class GetRevenueBreakdownTests {

        @Test
        @DisplayName("Should return cached response on Redis cache hit")
        void getRevenueBreakdown_CacheHit_ReturnsCached() throws Exception {
            RevenueStatsResponses.RevenueBreakdownResponse expected = new RevenueStatsResponses.RevenueBreakdownResponse(
                    List.of(), new RevenueStatsResponses.TransactionStatusBreakdown(0L, 0L, 0L, 0L, 0L, 0.0), LocalDateTime.now()
            );

            when(redisService.get(anyString())).thenReturn("{}");
            when(objectMapper.readValue("{}", RevenueStatsResponses.RevenueBreakdownResponse.class)).thenReturn(expected);

            RevenueStatsResponses.RevenueBreakdownResponse actual = revenueStatsService.getRevenueBreakdown(defaultRequest);

            assertThat(actual).isEqualTo(expected);
            verifyNoInteractions(revenueStatsQueryService);
        }

        @Test
        @DisplayName("Should calculate plan breakdown and transaction status breakdown on cache miss")
        void getRevenueBreakdown_CacheMiss_CalculatesMetrics() throws Exception {
            when(redisService.get(anyString())).thenReturn(null);

            // Plan breakdown
            PlanRevenueStat planProj = mock(PlanRevenueStat.class);
            when(planProj.getPlanId()).thenReturn(1L);
            when(planProj.getPlanName()).thenReturn("Premium 1 Year");
            when(planProj.getTotalAmount()).thenReturn(BigDecimal.valueOf(1_000_000));
            when(planProj.getTxCount()).thenReturn(5L);

            when(revenueStatsQueryService.getRevenueBreakdownByPlan(any(), any())).thenReturn(List.of(planProj));

            // Status breakdown
            TransactionStatusCount statusSuccess = mock(TransactionStatusCount.class);
            when(statusSuccess.getStatus()).thenReturn(PaymentTransactionStatus.SUCCESS);
            when(statusSuccess.getCountVal()).thenReturn(8L);

            TransactionStatusCount statusFailed = mock(TransactionStatusCount.class);
            when(statusFailed.getStatus()).thenReturn(PaymentTransactionStatus.FAILED);
            when(statusFailed.getCountVal()).thenReturn(2L);

            when(revenueStatsQueryService.countTransactionsByStatus(any(), any()))
                    .thenReturn(List.of(statusSuccess, statusFailed));

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            RevenueStatsResponses.RevenueBreakdownResponse actual = revenueStatsService.getRevenueBreakdown(defaultRequest);

            assertThat(actual.byPlan()).hasSize(1);
            assertThat(actual.byPlan().get(0).planName()).isEqualTo("Premium 1 Year");
            assertThat(actual.byPlan().get(0).percentage()).isEqualTo(100.0);

            RevenueStatsResponses.TransactionStatusBreakdown status = actual.statusBreakdown();
            assertThat(status.totalTransactions()).isEqualTo(10L);
            assertThat(status.successCount()).isEqualTo(8L);
            assertThat(status.failedCount()).isEqualTo(2L);
            assertThat(status.pendingCount()).isEqualTo(0L);
            assertThat(status.refundedCount()).isEqualTo(0L);
            // 8 / 10 = 80.0%
            assertThat(status.successRate()).isEqualTo(80.0);

            verify(redisService).save(anyString(), anyString(), eq(5L));
        }

        @Test
        @DisplayName("Should return 0.0 success rate when total transactions is 0")
        void getRevenueBreakdown_ZeroTransactions_SuccessRateIsZero() {
            when(redisService.get(anyString())).thenReturn(null);
            when(revenueStatsQueryService.getRevenueBreakdownByPlan(any(), any())).thenReturn(Collections.emptyList());
            when(revenueStatsQueryService.countTransactionsByStatus(any(), any())).thenReturn(Collections.emptyList());

            RevenueStatsResponses.RevenueBreakdownResponse actual = revenueStatsService.getRevenueBreakdown(defaultRequest);

            assertThat(actual.statusBreakdown().totalTransactions()).isEqualTo(0L);
            assertThat(actual.statusBreakdown().successRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 percentage when total plan revenue is zero")
        void getRevenueBreakdown_ZeroTotalPlanRevenue_PercentageIsZero() {
            when(redisService.get(anyString())).thenReturn(null);

            PlanRevenueStat planProj = mock(PlanRevenueStat.class);
            when(planProj.getPlanId()).thenReturn(1L);
            when(planProj.getPlanName()).thenReturn("Free Trial");
            when(planProj.getTotalAmount()).thenReturn(BigDecimal.ZERO);
            when(planProj.getTxCount()).thenReturn(0L);

            when(revenueStatsQueryService.getRevenueBreakdownByPlan(any(), any())).thenReturn(List.of(planProj));
            when(revenueStatsQueryService.countTransactionsByStatus(any(), any())).thenReturn(Collections.emptyList());

            RevenueStatsResponses.RevenueBreakdownResponse actual = revenueStatsService.getRevenueBreakdown(defaultRequest);

            assertThat(actual.byPlan().get(0).percentage()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getSubscriptionStats")
    class GetSubscriptionStatsTests {

        @Test
        @DisplayName("Should return cached response on Redis cache hit")
        void getSubscriptionStats_CacheHit_ReturnsCached() throws Exception {
            RevenueStatsResponses.SubscriptionStatsResponse expected = new RevenueStatsResponses.SubscriptionStatsResponse(
                    10L, 10L, 100L, 10.0, List.of(), LocalDateTime.now()
            );

            when(redisService.get("admin:stats:subscriptions:latest")).thenReturn("{}");
            when(objectMapper.readValue("{}", RevenueStatsResponses.SubscriptionStatsResponse.class)).thenReturn(expected);

            RevenueStatsResponses.SubscriptionStatsResponse actual = revenueStatsService.getSubscriptionStats();

            assertThat(actual).isEqualTo(expected);
            verifyNoInteractions(subscriptionService);
        }

        @Test
        @DisplayName("Should calculate subscription metrics and premium rate on cache miss")
        void getSubscriptionStats_CacheMiss_CalculatesMetrics() throws Exception {
            when(redisService.get(anyString())).thenReturn(null);

            PlanSubscriptionStat stat = mock(PlanSubscriptionStat.class);
            when(stat.getPlanId()).thenReturn(10L);
            when(stat.getPlanName()).thenReturn("Premium Pro");
            when(stat.getActiveCount()).thenReturn(50L);

            when(subscriptionService.countActiveSubscriptionsByPlan(any())).thenReturn(List.of(stat));
            when(subscriptionService.countTotalActiveSubscriptions(any())).thenReturn(50L);
            when(subscriptionService.countDistinctActivePremiumUsers(any())).thenReturn(45L);
            when(userService.countTotalUsers()).thenReturn(150L);

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            RevenueStatsResponses.SubscriptionStatsResponse actual = revenueStatsService.getSubscriptionStats();

            assertThat(actual.totalActiveSubscriptions()).isEqualTo(50L);
            assertThat(actual.totalPremiumUsers()).isEqualTo(45L);
            assertThat(actual.totalSystemUsers()).isEqualTo(150L);
            // 45 / 150 = 30.0%
            assertThat(actual.premiumRate()).isEqualTo(30.0);
            assertThat(actual.activeByPlan()).hasSize(1);
            assertThat(actual.activeByPlan().get(0).percentage()).isEqualTo(100.0);

            verify(redisService).save(eq("admin:stats:subscriptions:latest"), anyString(), eq(5L));
        }

        @Test
        @DisplayName("Should return 0.0 premiumRate when total system users is 0")
        void getSubscriptionStats_ZeroSystemUsers_PremiumRateIsZero() {
            when(redisService.get(anyString())).thenReturn(null);
            when(subscriptionService.countActiveSubscriptionsByPlan(any())).thenReturn(Collections.emptyList());
            when(subscriptionService.countTotalActiveSubscriptions(any())).thenReturn(0L);
            when(subscriptionService.countDistinctActivePremiumUsers(any())).thenReturn(0L);
            when(userService.countTotalUsers()).thenReturn(0L);

            RevenueStatsResponses.SubscriptionStatsResponse actual = revenueStatsService.getSubscriptionStats();

            assertThat(actual.totalSystemUsers()).isEqualTo(0L);
            assertThat(actual.premiumRate()).isEqualTo(0.0);
        }
    }
}
