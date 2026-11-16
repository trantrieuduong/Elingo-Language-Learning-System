package com.elingo.speaking.websocket;

import com.elingo.common.websocket.AbstractWsSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Tập trung mọi lệnh gửi event tới client qua /user/queue/speaking
 * Không nơi nào khác gọi SimpMessagingTemplate trực tiếp cho speaking
 */
@Component
public class SpeakingEventPublisher extends AbstractWsSender {

    public SpeakingEventPublisher(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @Override
    protected String userQueueDestination() {
        return "/queue/speaking";
    }

    @Override
    public String destinationPrefix() {
        return "/app/speaking";
    }
}
