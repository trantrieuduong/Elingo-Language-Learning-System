package com.elingo.notification.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.event.NotificationEvent;
import com.elingo.notification.dto.response.UnreadCountResponse;
import com.elingo.notification.dto.response.NotificationResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    NotificationResponse create(NotificationEvent event);
    PageResponse<NotificationResponse> getAll(Long userId, Pageable pageable);
    UnreadCountResponse countUnread(Long userId);
    void markRead(Long userId, Long notificationId);
    void markAllRead(Long userId);
}
