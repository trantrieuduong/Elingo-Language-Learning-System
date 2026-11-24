package com.elingo.admin.stats.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.RevenueStatsResponses;
import com.elingo.admin.stats.service.RevenueStatsService;
import com.elingo.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminApiPaths.STATS_REVENUE)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Stats Revenue",
        description = "Administrative analytics endpoints for revenue, payment transactions, and subscription plans")
public class RevenueStatsController {

    private final RevenueStatsService revenueStatsService;

    @GetMapping("/timeseries")
    @Operation(
            summary = "Get revenue and successful transaction time series by day or month",
            description = "Retrieves continuous revenue and successful transaction volumes across the specified date range. Gaps without transactions are zero-filled automatically."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Revenue time-series retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "RevenueTimeSeriesSuccessResponse",
                            summary = "Sample revenue time-series response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "totalRevenue": 45000000.00,
                                        "totalSuccessTransactions": 150,
                                        "currency": "VND",
                                        "points": [
                                          {
                                            "date": "2026-11-01",
                                            "revenue": 1500000.00,
                                            "transactionCount": 5,
                                            "cumulativeRevenue": 1500000.00
                                          },
                                          {
                                            "date": "2026-11-02",
                                            "revenue": 0.00,
                                            "transactionCount": 0,
                                            "cumulativeRevenue": 1500000.00
                                          },
                                          {
                                            "date": "2026-11-03",
                                            "revenue": 3000000.00,
                                            "transactionCount": 10,
                                            "cumulativeRevenue": 4500000.00
                                          }
                                        ],
                                        "generatedAt": "2026-11-20T21:00:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid date range parameters"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<RevenueStatsResponses.RevenueTimeSeriesResponse> getRevenueTimeSeries(
            @Valid @ParameterObject StatsDateRangeRequest request) {
        return ApiResponse.<RevenueStatsResponses.RevenueTimeSeriesResponse>builder()
                .success(true)
                .data(revenueStatsService.getRevenueTimeSeries(request))
                .build();
    }

    @GetMapping("/breakdown")
    @Operation(
            summary = "Get revenue breakdown by plan and transaction status",
            description = "Calculates revenue distribution and percentages grouped by subscription plan alongside payment transaction status counts and overall success rate."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Revenue breakdown retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "RevenueBreakdownSuccessResponse",
                            summary = "Sample revenue breakdown response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "byPlan": [
                                          {
                                            "planId": 1,
                                            "planName": "Premium 1 Month",
                                            "revenue": 15000000.00,
                                            "transactionCount": 100,
                                            "percentage": 33.33
                                          },
                                          {
                                            "planId": 2,
                                            "planName": "Premium 1 Year",
                                            "revenue": 30000000.00,
                                            "transactionCount": 50,
                                            "percentage": 66.67
                                          }
                                        ],
                                        "statusBreakdown": {
                                          "totalTransactions": 170,
                                          "successCount": 150,
                                          "pendingCount": 12,
                                          "failedCount": 6,
                                          "refundedCount": 2,
                                          "successRate": 88.24
                                        },
                                        "generatedAt": "2026-11-20T21:00:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid date range parameters"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<RevenueStatsResponses.RevenueBreakdownResponse> getRevenueBreakdown(
            @Valid @ParameterObject StatsDateRangeRequest request) {
        return ApiResponse.<RevenueStatsResponses.RevenueBreakdownResponse>builder()
                .success(true)
                .data(revenueStatsService.getRevenueBreakdown(request))
                .build();
    }

    @GetMapping("/subscriptions")
    @Operation(
            summary = "Get active subscription statistics and premium penetration rate",
            description = "Provides live statistics on active subscriptions grouped by plan, unique active premium users, and overall premium penetration rate across all registered users."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Subscription statistics retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "SubscriptionStatsSuccessResponse",
                            summary = "Sample active subscription statistics response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "totalActiveSubscriptions": 380,
                                        "totalPremiumUsers": 350,
                                        "totalSystemUsers": 2500,
                                        "premiumRate": 14.0,
                                        "activeByPlan": [
                                          {
                                            "planId": 1,
                                            "planName": "Premium 1 Month",
                                            "activeCount": 220,
                                            "percentage": 57.89
                                          },
                                          {
                                            "planId": 2,
                                            "planName": "Premium 1 Year",
                                            "activeCount": 160,
                                            "percentage": 42.11
                                          }
                                        ],
                                        "generatedAt": "2026-11-20T21:00:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<RevenueStatsResponses.SubscriptionStatsResponse> getSubscriptionStats() {
        return ApiResponse.<RevenueStatsResponses.SubscriptionStatsResponse>builder()
                .success(true)
                .data(revenueStatsService.getSubscriptionStats())
                .build();
    }
}
