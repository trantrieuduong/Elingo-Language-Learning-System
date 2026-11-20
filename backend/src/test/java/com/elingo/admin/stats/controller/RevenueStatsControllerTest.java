package com.elingo.admin.stats.controller;

import com.elingo.admin.stats.dto.response.RevenueStatsResponses;
import com.elingo.admin.stats.service.RevenueStatsService;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.common.exception.HttpExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RevenueStatsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RevenueStatsService revenueStatsService;

    @InjectMocks
    private RevenueStatsController revenueStatsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(revenueStatsController)
                .setControllerAdvice(new GlobalExceptionHandler(), new HttpExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("getRevenueTimeSeries")
    class GetRevenueTimeSeriesTests {

        @Test
        @DisplayName("Should return 200 and timeseries data when request is valid")
        void getRevenueTimeSeries_Success() throws Exception {
            RevenueStatsResponses.RevenueTimeSeriesResponse response = new RevenueStatsResponses.RevenueTimeSeriesResponse(
                    BigDecimal.valueOf(500_000), 5L, "VND", List.of(), LocalDateTime.now()
            );

            when(revenueStatsService.getRevenueTimeSeries(any())).thenReturn(response);

            mockMvc.perform(get("/admin/stats/revenue/timeseries")
                            .param("from", "2026-11-01")
                            .param("to", "2026-11-05")
                            .param("granularity", "DAY")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalRevenue", is(500000)))
                    .andExpect(jsonPath("$.data.totalSuccessTransactions", is(5)))
                    .andExpect(jsonPath("$.data.currency", is("VND")));

            verify(revenueStatsService).getRevenueTimeSeries(any());
        }

        @Test
        @DisplayName("Should return 400 when date range is invalid")
        void getRevenueTimeSeries_InvalidDateRange_Returns400() throws Exception {
            mockMvc.perform(get("/admin/stats/revenue/timeseries")
                            .param("from", "2026-11-20")
                            .param("to", "2026-11-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }
    }

    @Nested
    @DisplayName("getRevenueBreakdown")
    class GetRevenueBreakdownTests {

        @Test
        @DisplayName("Should return 200 and revenue breakdown data")
        void getRevenueBreakdown_Success() throws Exception {
            RevenueStatsResponses.RevenueBreakdownResponse response = new RevenueStatsResponses.RevenueBreakdownResponse(
                    List.of(new RevenueStatsResponses.PlanRevenue(1L, "Monthly", BigDecimal.valueOf(100_000), 2L, 100.0)),
                    new RevenueStatsResponses.TransactionStatusBreakdown(2L, 2L, 0L, 0L, 0L, 100.0),
                    LocalDateTime.now()
            );

            when(revenueStatsService.getRevenueBreakdown(any())).thenReturn(response);

            mockMvc.perform(get("/admin/stats/revenue/breakdown")
                            .param("from", "2026-11-01")
                            .param("to", "2026-11-05"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.byPlan", hasSize(1)))
                    .andExpect(jsonPath("$.data.statusBreakdown.successRate", is(100.0)));
        }
    }

    @Nested
    @DisplayName("getSubscriptionStats")
    class GetSubscriptionStatsTests {

        @Test
        @DisplayName("Should return 200 and subscription statistics")
        void getSubscriptionStats_Success() throws Exception {
            RevenueStatsResponses.SubscriptionStatsResponse response = new RevenueStatsResponses.SubscriptionStatsResponse(
                    50L, 45L, 100L, 45.0, List.of(), LocalDateTime.now()
            );

            when(revenueStatsService.getSubscriptionStats()).thenReturn(response);

            mockMvc.perform(get("/admin/stats/revenue/subscriptions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalActiveSubscriptions", is(50)))
                    .andExpect(jsonPath("$.data.totalPremiumUsers", is(45)))
                    .andExpect(jsonPath("$.data.totalSystemUsers", is(100)))
                    .andExpect(jsonPath("$.data.premiumRate", is(45.0)));
        }
    }
}
