package com.elingo.notification.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.event.NotificationEvent;
import com.elingo.notification.dto.response.UnreadCountResponse;
import com.elingo.notification.dto.response.NotificationResponse;
import com.elingo.notification.entity.Notification;
import com.elingo.notification.mapper.NotificationMapper;
import com.elingo.notification.repository.NotificationRepository;
import com.elingo.notification.service.NotificationService;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "NOTIFICATION-SERVICE")
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper mapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public NotificationResponse create(NotificationEvent event) {
        Notification n = Notification.builder()
                .user(userRepository.getReferenceById(event.recipientId()))
                .type(event.type())
                .title(event.type().renderDefaultTitle(event.args()))
                .params(event.args())
                .refType(event.type().getRefType())
                .refId(event.refId())
                .build();
        Notification saved = notificationRepository.save(n);
        log.info("Notification saved id={} type={} recipientId={} refType={} refId={}",
                saved.getId(), event.type(), event.recipientId(), event.type().getRefType(), event.refId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAll(Long userId, Pageable pageable) {
        Page<Notification> notificationPage = notificationRepository
                .findByUserId(userId, pageable);
        return PageResponse.of(notificationPage.map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse countUnread(Long userId) {
        return new UnreadCountResponse(notificationRepository
                .countByUserIdAndIsReadIsFalse(userId));
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        notificationRepository.markRead(userId, notificationId);
        log.info("Notification marked read id={}", notificationId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
        log.info("All notifications marked read");
    }
}
