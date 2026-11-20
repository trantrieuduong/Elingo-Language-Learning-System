package com.elingo.admin.stats.controller;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.RevenueStatsResponses;
import com.elingo.admin.stats.service.RevenueStatsService;
import com.elingo.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/stats/revenue")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Revenue & Premium Stats",
        description = "Administrative analytics endpoints for revenue and subscriptions")
public class RevenueStatsController {

    private final RevenueStatsService revenueStatsService;

    @GetMapping("/timeseries")
    @Operation(summary = "Get revenue and successful transaction time series by day or month")
    public ApiResponse<RevenueStatsResponses.RevenueTimeSeriesResponse> getRevenueTimeSeries(
            @Valid @ParameterObject StatsDateRangeRequest request) {
        return ApiResponse.<RevenueStatsResponses.RevenueTimeSeriesResponse>builder()
                .success(true)
                .data(revenueStatsService.getRevenueTimeSeries(request))
                .build();
    }

    @GetMapping("/breakdown")
    @Operation(summary = "Get revenue breakdown by plan and transaction status")
    public ApiResponse<RevenueStatsResponses.RevenueBreakdownResponse> getRevenueBreakdown(
            @Valid @ParameterObject StatsDateRangeRequest request) {
        return ApiResponse.<RevenueStatsResponses.RevenueBreakdownResponse>builder()
                .success(true)
                .data(revenueStatsService.getRevenueBreakdown(request))
                .build();
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "Get active subscription statistics and premium penetration rate")
    public ApiResponse<RevenueStatsResponses.SubscriptionStatsResponse> getSubscriptionStats() {
        return ApiResponse.<RevenueStatsResponses.SubscriptionStatsResponse>builder()
                .success(true)
                .data(revenueStatsService.getSubscriptionStats())
                .build();
    }
}
