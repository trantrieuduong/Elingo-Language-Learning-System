package com.elingo.common.event;

import com.elingo.notification.entity.NotificationType;

import java.util.Map;
import java.util.Objects;

public record NotificationEvent(
        Long recipientId,
        NotificationType type,
        Long refId,
        Map<String, String> args
) {
    public NotificationEvent {
        Objects.requireNonNull(recipientId, "recipientId");
        Objects.requireNonNull(type, "type");
        args = args == null ? Map.of() : Map.copyOf(args);
    }

    public static NotificationEvent postLiked(long authorId, long postId, String actor) {
        return new NotificationEvent(authorId,
                NotificationType.POST_LIKED,
                postId,
                Map.of("actor", actor)
        );
    }

    public static NotificationEvent postCommented(long authorId, long postId, String actor) {
        return new NotificationEvent(authorId,
                NotificationType.POST_COMMENTED,
                postId,
                Map.of("actor", actor)
        );
    }

    public static NotificationEvent commentReplied(long commentOwnerId, long postId, String actor) {
        return new NotificationEvent(commentOwnerId,
                NotificationType.COMMENT_REPLIED,
                postId,
                Map.of("actor", actor)
        );
    }

    public static NotificationEvent badgeEarned(long userId, long badgeId, String badgeName) {
        return new NotificationEvent(userId,
                NotificationType.BADGE_EARNED,
                badgeId,
                Map.of("badgeName", badgeName)
        );
    }

    public static NotificationEvent subscriptionActivated(long userId, long subscriptionId,
                                                          String planName, String expiresAt) {
        return new NotificationEvent(userId,
                NotificationType.SUBSCRIPTION_ACTIVATED,
                subscriptionId,
                Map.of("planName", planName,
                        "expiresAt", expiresAt)
        );
    }

    public static NotificationEvent subscriptionExpiring(long userId, long subscriptionId,
                                                          String planName, String expiresAt) {
        return new NotificationEvent(userId,
                NotificationType.SUBSCRIPTION_EXPIRING,
                subscriptionId,
                Map.of("planName", planName,
                        "expiresAt", expiresAt)
        );
    }

    public static NotificationEvent reportResolved(long reporterId, long reportId) {
        return new NotificationEvent(reporterId,
                NotificationType.REPORT_RESOLVED,
                reportId,
                Map.of()
        );
    }
}
