package com.elingo.admin.stats.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.CommunityStatsResponses;
import com.elingo.admin.stats.service.CommunityStatsService;
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
@RequestMapping(AdminApiPaths.STATS_COMMUNITY)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Stats Community",
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
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "SocialStatsSuccessResponse",
                            summary = "Sample social engagement response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "totalPosts": 1200,
                                        "totalLikes": 3500,
                                        "totalComments": 1800,
                                        "byStatus": [
                                          { "category": "PUBLISHED", "count": 1050, "percentage": 87.5 },
                                          { "category": "PENDING", "count": 50, "percentage": 4.17 },
                                          { "category": "NEEDS_REVIEW", "count": 30, "percentage": 2.5 },
                                          { "category": "REJECTED", "count": 70, "percentage": 5.83 }
                                        ],
                                        "byGroup": [
                                          {
                                            "groupId": 1,
                                            "groupName": "English Grammar Q&A",
                                            "postCount": 600,
                                            "percentage": 50.0
                                          },
                                          {
                                            "groupId": 2,
                                            "groupName": "Daily Discussion",
                                            "postCount": 450,
                                            "percentage": 37.5
                                          }
                                        ],
                                        "engagementSeries": [
                                          {
                                            "date": "2026-11-01",
                                            "likeCount": 120,
                                            "commentCount": 45,
                                            "totalInteractions": 165
                                          },
                                          {
                                            "date": "2026-11-02",
                                            "likeCount": 0,
                                            "commentCount": 0,
                                            "totalInteractions": 0
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
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "ModerationStatsSuccessResponse",
                            summary = "Sample moderation analytics response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "actions": {
                                          "autoApproveCount": 850,
                                          "autoRejectCount": 120,
                                          "flagForReviewCount": 80,
                                          "manualApproveCount": 65,
                                          "manualRejectCount": 15,
                                          "totalActions": 1130,
                                          "automationRate": 85.84
                                        },
                                        "aiLabelDistribution": [
                                          { "category": "CLEAN", "count": 850, "percentage": 80.95 },
                                          { "category": "SPAM", "count": 120, "percentage": 11.43 },
                                          { "category": "TOXIC", "count": 80, "percentage": 7.62 }
                                        ],
                                        "avgManualReviewSeconds": 145.5,
                                        "backlog": {
                                          "pendingCount": 5,
                                          "needsReviewCount": 12,
                                          "totalBacklog": 17
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
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "ReportStatsSuccessResponse",
                            summary = "Sample violation reports response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "totalReports": 250,
                                        "pendingReports": 35,
                                        "resolutionRate": 86.0,
                                        "byStatus": [
                                          { "category": "RESOLVED", "count": 180, "percentage": 72.0 },
                                          { "category": "REJECTED", "count": 35, "percentage": 14.0 },
                                          { "category": "PENDING", "count": 35, "percentage": 14.0 }
                                        ],
                                        "byTargetType": [
                                          { "category": "POST", "count": 140, "percentage": 56.0 },
                                          { "category": "COMMENT", "count": 70, "percentage": 28.0 },
                                          { "category": "USER", "count": 40, "percentage": 16.0 }
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
    public ApiResponse<CommunityStatsResponses.ReportStatsResponse> getReportStats(
            @Valid @ParameterObject StatsDateRangeRequest request
    ) {
        return ApiResponse.<CommunityStatsResponses.ReportStatsResponse>builder()
                .success(true)
                .data(communityStatsService.getReportStats(request))
                .build();
    }
}
