package com.elingo.community.moderation.dto.projection;

public record ModerationActionCounts(
        long autoApproveCount,
        long autoRejectCount,
        long flagForReviewCount,
        long manualApproveCount,
        long manualRejectCount,
        long totalActions
) {}
