package com.elingo.admin.stats.controller;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.UserStatsResponses;
import com.elingo.admin.stats.service.UserStatsService;
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
@RequestMapping("/admin/stats/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin User Stats",
        description = "Administrative analytics endpoints for user growth, activity, and composition")
public class UserStatsController {

    private final UserStatsService userStatsService;

    @GetMapping("/growth")
    @Operation(summary = "Get user growth statistics by day/week/month")
    public ApiResponse<UserStatsResponses.UserGrowthResponse> getUserGrowth(
            @Valid @ParameterObject StatsDateRangeRequest request) {
        return ApiResponse.<UserStatsResponses.UserGrowthResponse>builder()
                .success(true)
                .data(userStatsService.getUserGrowth(request))
                .build();
    }

    @GetMapping("/activity")
    @Operation(summary = "Get user activity statistics (DAU, WAU, MAU)",
            description = "Calculates daily active users (DAU) from distinct user_id in xp_events alongside WAU, MAU, and stickiness ratio")
    public ApiResponse<UserStatsResponses.UserActivityResponse> getUserActivity(
            @Valid @ParameterObject StatsDateRangeRequest request
    ) {
        return ApiResponse.<UserStatsResponses.UserActivityResponse>builder()
                .success(true)
                .data(userStatsService.getUserActivity(request))
                .build();
    }

    @GetMapping("/breakdown")
    @Operation(summary = "Get comprehensive user breakdown")
    public ApiResponse<UserStatsResponses.UserBreakdownResponse> getUserBreakdown() {
        return ApiResponse.<UserStatsResponses.UserBreakdownResponse>builder()
                .success(true)
                .data(userStatsService.getUserBreakdown())
                .build();
    }
}
