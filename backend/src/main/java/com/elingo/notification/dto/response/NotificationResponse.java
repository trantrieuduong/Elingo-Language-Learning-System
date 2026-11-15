package com.elingo.notification.dto.response;

import com.elingo.notification.entity.NotificationRefType;
import com.elingo.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String titleKey,
        String title,
        Map<String, String> params,
        NotificationRefType refType,
        Long refId,
        Boolean read,
        LocalDateTime createdAt
) {
}
