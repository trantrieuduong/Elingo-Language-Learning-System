package com.elingo.admin.stats.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CommunityStatsResponses {
    private CommunityStatsResponses() {}

    @Schema(description = "Daily social interaction counts (likes and comments)")
    public record SocialEngagementPoint(
            @Schema(description = "Date of engagement (YYYY-MM-DD)", example = "2026-11-20")
            LocalDate date,

            @Schema(description = "Total post likes received on this date", example = "120")
            Long likeCount,

            @Schema(description = "Total comments created on this date", example = "45")
            Long commentCount,

            @Schema(description = "Total likes plus comments on this date", example = "165")
            Long totalInteractions
    ) {}

    @Schema(description = "Group post distribution item")
    public record GroupPostShare(
            @Schema(description = "Unique identifier of the post group", example = "3")
            Long groupId,

            @Schema(description = "Name of the post group", example = "IELTS Speaking Practice")
            String groupName,

            @Schema(description = "Number of posts in this group", example = "420")
            Long postCount,

            @Schema(description = "Percentage share of posts in this group over total posts", example = "35.0")
            Double percentage
    ) {}

    @Schema(description = "Social content breakdown and interaction time series")
    public record SocialStatsResponse(
            @Schema(description = "Total posts across all groups and statuses", example = "1200")
            Long totalPosts,

            @Schema(description = "Total likes received across the query period", example = "3500")
            Long totalLikes,

            @Schema(description = "Total comments created across the query period", example = "1800")
            Long totalComments,

            @Schema(description = "Distribution of posts grouped by post status")
            List<CategoryShare<String>> byStatus,

            @Schema(description = "Distribution of posts grouped by discussion group")
            List<GroupPostShare> byGroup,

            @Schema(description = "Continuous daily social engagement series with zero-filled gaps")
            List<SocialEngagementPoint> engagementSeries,

            @Schema(description = "Timestamp when this statistic report was generated", example = "2026-11-20T21:00:00")
            LocalDateTime generatedAt
    ) {}

    @Schema(description = "Breakdown of moderation actions and automation efficiency")
    public record ModerationActionBreakdown(
            @Schema(description = "Number of posts automatically approved by AI", example = "850")
            Long autoApproveCount,

            @Schema(description = "Number of posts automatically rejected by AI", example = "120")
            Long autoRejectCount,

            @Schema(description = "Number of posts flagged by AI for human review", example = "80")
            Long flagForReviewCount,

            @Schema(description = "Number of posts manually approved by admin moderators", example = "65")
            Long manualApproveCount,

            @Schema(description = "Number of posts manually rejected by admin moderators", example = "15")
            Long manualRejectCount,

            @Schema(description = "Total moderation decisions recorded during the period", example = "1130")
            Long totalActions,

            @Schema(description = "AI automation resolution rate as percentage ((autoApprove + autoReject) / totalActions * 100)", example = "85.84")
            Double automationRate
    ) {}

    @Schema(description = "Current moderation review backlog metrics")
    public record ModerationBacklog(
            @Schema(description = "Number of posts currently pending initial AI evaluation", example = "5")
            Long pendingCount,

            @Schema(description = "Number of posts currently awaiting manual review by admin", example = "12")
            Long needsReviewCount,

            @Schema(description = "Total posts currently waiting in moderation pipeline", example = "17")
            Long totalBacklog
    ) {}

    @Schema(description = "Community moderation analytics response")
    public record ModerationStatsResponse(
            @Schema(description = "Breakdown of moderation decisions and AI automation rate")
            ModerationActionBreakdown actions,

            @Schema(description = "Distribution of AI classification labels (SPAM, TOXIC, CLEAN, etc.)")
            List<CategoryShare<String>> aiLabelDistribution,

            @Schema(description = "Average turnaround time for manual review in seconds", example = "145.5")
            Double avgManualReviewSeconds,

            @Schema(description = "Current real-time moderation backlog snapshot")
            ModerationBacklog backlog,

            @Schema(description = "Timestamp when this statistic report was generated", example = "2026-11-20T21:00:00")
            LocalDateTime generatedAt
    ) {}

    @Schema(description = "Community violation report statistics response")
    public record ReportStatsResponse(
            @Schema(description = "Total violation reports submitted during the period", example = "250")
            Long totalReports,

            @Schema(description = "Number of reports currently pending resolution", example = "35")
            Long pendingReports,

            @Schema(description = "Percentage of reports resolved or rejected over total reports", example = "86.0")
            Double resolutionRate,

            @Schema(description = "Distribution of reports grouped by status (PENDING, RESOLVED, REJECTED)")
            List<CategoryShare<String>> byStatus,

            @Schema(description = "Distribution of reports grouped by target entity type (POST, COMMENT, USER, DECK, LESSON)")
            List<CategoryShare<String>> byTargetType,

            @Schema(description = "Timestamp when this statistic report was generated", example = "2026-11-20T21:00:00")
            LocalDateTime generatedAt
    ) {}
}
