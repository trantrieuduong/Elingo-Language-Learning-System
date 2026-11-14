package com.elingo.notification.publisher;

import com.elingo.common.websocket.AbstractWsSender;
import com.elingo.notification.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "NOTIFICATION-EVENT-PUBLISHER")
public class NotificationEventPublisher extends AbstractWsSender {

    public NotificationEventPublisher(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @Override
    protected String userQueueDestination() {
        return "/queue/notifications";
    }

    public void sendNotification(Long recipientId, NotificationResponse notification) {
        try {
            sendToUser(recipientId, NotificationEvents.NOTIFICATION_NEW, notification);
            log.info("Notification pushed via websocket id={} type={} recipientId={}",
                    notification.id(), notification.type(), recipientId);
        } catch (Exception ex) {
            log.warn("Notification websocket push failed id={} recipientId={}",
                    notification.id(), recipientId, ex);
        }
    }
}
