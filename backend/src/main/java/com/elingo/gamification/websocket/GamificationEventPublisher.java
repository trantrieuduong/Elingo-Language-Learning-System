package com.elingo.gamification.websocket;

import com.elingo.common.websocket.AbstractWsSender;
import com.elingo.gamification.dto.response.XpGainedResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Service gửi các sự kiện WebSocket liên quan đến Gamification tới client.
 */
@Component
public class GamificationEventPublisher extends AbstractWsSender {

    public GamificationEventPublisher(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @Override
    protected String userQueueDestination() {
        return "/queue/xp-event";
    }

    /**
     * Gửi sự kiện cộng điểm kinh nghiệm (XP) cho người dùng.
     */
    public void sendXpGained(Long userId, int xpGained) {
        sendToUser(userId, GamificationEvents.XP_GAINED, new XpGainedResponse(xpGained));
    }
}
