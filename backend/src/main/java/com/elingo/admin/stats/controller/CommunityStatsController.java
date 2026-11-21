package com.elingo.admin.stats.controller;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.CommunityStatsResponses;
import com.elingo.admin.stats.service.CommunityStatsService;
import com.elingo.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
@RequestMapping("/admin/stats/community")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Community Stats",
        description = "Administrative analytics endpoints for social engagements, content moderation, and violation reports")
public class CommunityStatsController {

    private final CommunityStatsService communityStatsService;

    @GetMapping("/social")
    @Operation(
            summary = "Get social post distribution and daily engagement metrics",
            description = "Retrieves distribution of posts by status and group, alongside daily continuous like and comment time-series with zero-filled gaps."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Social statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid date range parameters"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or expired authentication token"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<CommunityStatsResponses.SocialStatsResponse> getSocialStats(
            @Valid @ParameterObject StatsDateRangeRequest request
    ) {
        return ApiResponse.<CommunityStatsResponses.SocialStatsResponse>builder()
                .success(true)
                .data(communityStatsService.getSocialStats(request))
                .build();
    }

    @GetMapping("/moderation")
    @Operation(
            summary = "Get moderation performance, AI label distributions, and review backlog",
            description = "Calculates auto vs manual moderation decision volumes, distribution of AI detection labels, average manual review turnaround duration, and current pending review backlog."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Moderation statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid date range parameters"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or expired authentication token"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<CommunityStatsResponses.ModerationStatsResponse> getModerationStats(
            @Valid @ParameterObject StatsDateRangeRequest request
    ) {
        return ApiResponse.<CommunityStatsResponses.ModerationStatsResponse>builder()
                .success(true)
                .data(communityStatsService.getModerationStats(request))
                .build();
    }

    @GetMapping("/reports")
    @Operation(
            summary = "Get user violation reports breakdown by status and target type",
            description = "Provides distribution of user reports by resolution status and reported target entity types (POST, COMMENT, USER, DECK, LESSON) with resolution rate."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Report statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid date range parameters"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or expired authentication token"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<CommunityStatsResponses.ReportStatsResponse> getReportStats(
            @Valid @ParameterObject StatsDateRangeRequest request
    ) {
        return ApiResponse.<CommunityStatsResponses.ReportStatsResponse>builder()
                .success(true)
                .data(communityStatsService.getReportStats(request))
                .build();
    }
}
