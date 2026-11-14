package com.elingo.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    POST_LIKED(NotificationRefType.POST, "{actor} liked your post"),
    POST_COMMENTED(NotificationRefType.POST, "{actor} commented on your post"),
    COMMENT_REPLIED(NotificationRefType.POST, "{actor} replied to your comment"),
    BADGE_EARNED(NotificationRefType.BADGE, "You earned the {badgeName} badge"),
    SUBSCRIPTION_ACTIVATED(NotificationRefType.SUBSCRIPTION, "Your {planName} plan is active until {expiresAt}"),
    SUBSCRIPTION_EXPIRING(NotificationRefType.SUBSCRIPTION, "Your {planName} plan will expire on {expiresAt}. To keep enjoying our features, please remember to renew it soon!"),
    REPORT_RESOLVED(NotificationRefType.REPORT, "Your report has been resolved");

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    private final NotificationRefType refType;
    private final String defaultTitleTemplate;

    public String titleKey() {
        return "notification." + name() + ".title";
    }

    /** Các param bắt buộc, suy ra từ placeholder trong template. */
    public Set<String> requiredParams() {
        Set<String> keys = new LinkedHashSet<>();
        Matcher m = PLACEHOLDER.matcher(defaultTitleTemplate);
        while (m.find()) keys.add(m.group(1));
        return keys;
    }

    public String renderDefaultTitle(Map<String, String> params) {
        String result = defaultTitleTemplate;
        for (Map.Entry<String, String> e : params.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue());
        }
        return result;
    }
}
