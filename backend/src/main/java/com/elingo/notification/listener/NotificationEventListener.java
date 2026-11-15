package com.elingo.notification.listener;

import com.elingo.common.event.NotificationEvent;
import com.elingo.notification.dto.response.NotificationResponse;
import com.elingo.notification.service.NotificationService;
import com.elingo.notification.publisher.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "NOTIFICATION-EVENT-LISTENER")
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(NotificationEvent event) {
        log.info("Notification event received type={} recipientId={} refId={}",
                event.type(), event.recipientId(), event.refId());

        try {
            NotificationResponse response = notificationService.create(event);
            notificationEventPublisher.sendNotification(event.recipientId(), response);
            log.info("Notification processed id={} type={} recipientId={}",
                    response.id(), event.type(), event.recipientId());
        } catch (Exception ex) {
            log.error("Notification processing failed type={} recipientId={} refId={}",
                    event.type(), event.recipientId(), event.refId(), ex);
        }
    }
}
