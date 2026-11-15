package com.elingo.community.moderation.model;

import com.elingo.community.entity.ModerationAction;

public record ModerationResult(ModerationAction action,
                               String label,
                               double score, // Điểm càng thấp là càng clean
                               String note) {
    public static ModerationResult clean() {
        return new ModerationResult(ModerationAction.AUTO_APPROVE, "clean", 0.0, null);
    }

    public ModerationResult worseOf(ModerationResult other) {
        int bySeverity = Integer.compare(severity(other.action), severity(action));
        if (bySeverity != 0) {
            return bySeverity > 0 ? other : this;
        }
        return other.score > score ? other : this;
    }

    /** Mức độ nghiêm trọng: AUTO_APPROVE < FLAG_FOR_REVIEW < AUTO_REJECT. */
    private static int severity(ModerationAction action) {
        return switch (action) {
            case AUTO_APPROVE -> 0;
            case FLAG_FOR_REVIEW -> 1;
            case AUTO_REJECT -> 2;
            case MANUAL_APPROVE, MANUAL_REJECT ->
                    throw new IllegalArgumentException("Manual actions are not produced by moderation: " + action);
        };
    }
}
