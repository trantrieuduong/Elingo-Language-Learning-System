package com.elingo.admin.stats.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CommunityStatsResponses {
    private CommunityStatsResponses() {}

    @Schema(description = "Daily social interaction counts (likes and comments)")
    public record SocialEngagementPoint(
            @Schema(description = "Date (YYYY-MM-DD)", example = "2026-11-20")
            LocalDate date,
            @Schema(description = "Total post likes received on this date", example = "120")
            Long likeCount,
            @Schema(description = "Total comments created on this date", example = "45")
            Long commentCount,
            @Schema(description = "Total likes plus comments on this date", example = "165")
            Long totalInteractions
    ) {}

    @Schema(description = "Group post distribution")
    public record GroupPostShare(
            Long groupId,
            String groupName,
            Long postCount,
            Double percentage
    ) {}

    @Schema(description = "Social content breakdown and interaction time series")
    public record SocialStatsResponse(
            Long totalPosts,
            Long totalLikes,
            Long totalComments,
            List<CategoryShare<String>> byStatus,
            List<GroupPostShare> byGroup,
            List<SocialEngagementPoint> engagementSeries,
            LocalDateTime generatedAt
    ) {}

    @Schema(description = "Breakdown of moderation actions")
    public record ModerationActionBreakdown(
            Long autoApproveCount,
            Long autoRejectCount,
            Long flagForReviewCount,
            Long manualApproveCount,
            Long manualRejectCount,
            Long totalActions,
            Double automationRate
    ) {}

    @Schema(description = "Current moderation backlog")
    public record ModerationBacklog(
            Long pendingCount,
            Long needsReviewCount,
            Long totalBacklog
    ) {}

    @Schema(description = "Community moderation analytics response")
    public record ModerationStatsResponse(
            ModerationActionBreakdown actions,
            List<CategoryShare<String>> aiLabelDistribution,
            Double avgManualReviewSeconds,
            ModerationBacklog backlog,
            LocalDateTime generatedAt
    ) {}

    @Schema(description = "Community report statistics response")
    public record ReportStatsResponse(
            Long totalReports,
            Long pendingReports,
            Double resolutionRate,
            List<CategoryShare<String>> byStatus,
            List<CategoryShare<String>> byTargetType,
            LocalDateTime generatedAt
    ) {}
}
