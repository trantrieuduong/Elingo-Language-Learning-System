package com.elingo.admin.stats.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.UserStatsResponses;
import com.elingo.admin.stats.service.UserStatsService;
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
@RequestMapping(AdminApiPaths.STATS_USERS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Stats Users",
        description = "Administrative analytics endpoints for user growth, activity, and composition")
public class UserStatsController {

    private final UserStatsService userStatsService;

    @GetMapping("/growth")
    @Operation(
            summary = "Get user growth statistics by day/week/month",
            description = "Retrieves continuous new user counts and cumulative total users grouped by the specified granularity (DAY, WEEK, MONTH). Missing periods are zero-filled automatically."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User growth time-series data retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UserGrowthSuccessResponse",
                            summary = "Sample user growth statistics response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "totalBeforeRange": 1520,
                                        "newUsers": [
                                          { "key": "2026-11-01", "value": 25 },
                                          { "key": "2026-11-02", "value": 30 },
                                          { "key": "2026-11-03", "value": 18 }
                                        ],
                                        "cumulativeUsers": [
                                          { "key": "2026-11-01", "value": 1545 },
                                          { "key": "2026-11-02", "value": 1575 },
                                          { "key": "2026-11-03", "value": 1593 }
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
            description = "Invalid date range parameters (from is after to, or range exceeds allowed limit for the chosen granularity)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<UserStatsResponses.UserGrowthResponse> getUserGrowth(
            @Valid @ParameterObject StatsDateRangeRequest request) {
        return ApiResponse.<UserStatsResponses.UserGrowthResponse>builder()
                .success(true)
                .data(userStatsService.getUserGrowth(request))
                .build();
    }

    @GetMapping("/activity")
    @Operation(
            summary = "Get user activity statistics (DAU, WAU, MAU)",
            description = "Calculates daily active users (DAU) from distinct user_id in xp_events alongside trailing 7-day WAU, 30-day MAU, and user engagement stickiness ratio."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User activity statistics retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UserActivitySuccessResponse",
                            summary = "Sample user activity statistics response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "currentDau": 145,
                                        "currentWau": 480,
                                        "currentMau": 1650,
                                        "stickinessPercentage": 8.79,
                                        "dauTimeSeries": [
                                          { "key": "2026-11-01", "value": 120 },
                                          { "key": "2026-11-02", "value": 135 },
                                          { "key": "2026-11-03", "value": 145 }
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
    public ApiResponse<UserStatsResponses.UserActivityResponse> getUserActivity(
            @Valid @ParameterObject StatsDateRangeRequest request
    ) {
        return ApiResponse.<UserStatsResponses.UserActivityResponse>builder()
                .success(true)
                .data(userStatsService.getUserActivity(request))
                .build();
    }

    @GetMapping("/breakdown")
    @Operation(
            summary = "Get comprehensive user breakdown",
            description = "Provides distribution of all users by email verification status, active/banned account status, premium subscription tier, and level progression."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User breakdown data retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UserBreakdownSuccessResponse",
                            summary = "Sample user breakdown response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "totalUsers": 2500,
                                        "verifiedCount": 2150,
                                        "verifiedPercentage": 86.0,
                                        "activeCount": 2450,
                                        "activePercentage": 98.0,
                                        "premiumCount": 350,
                                        "premiumPercentage": 14.0,
                                        "levelDistribution": [
                                          { "key": 1, "value": 850 },
                                          { "key": 2, "value": 620 },
                                          { "key": 3, "value": 410 },
                                          { "key": 4, "value": 280 },
                                          { "key": 5, "value": 190 },
                                          { "key": 6, "value": 150 }
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
    public ApiResponse<UserStatsResponses.UserBreakdownResponse> getUserBreakdown() {
        return ApiResponse.<UserStatsResponses.UserBreakdownResponse>builder()
                .success(true)
                .data(userStatsService.getUserBreakdown())
                .build();
    }
}
