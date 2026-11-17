package com.elingo.gamification.publisher;

import com.elingo.common.websocket.AbstractWsSender;
import com.elingo.gamification.dto.response.LevelUpWsResponse;
import com.elingo.gamification.dto.response.XpGainedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Service gửi các sự kiện WebSocket liên quan đến Gamification tới client.
 */
@Component
@Slf4j(topic = "GAMIFICATION-EVENT-PUBLISHER")
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
        try {
            sendToUser(userId, GamificationEvents.XP_GAINED, new XpGainedResponse(xpGained));
            log.info("Xp event pushed via websocket amount={}", xpGained);
        } catch (Exception ex) {
            log.warn("Xp event websocket push failed amount={}", xpGained, ex);
        }
    }

    /**
     * Gửi sự kiện thăng cấp (Level Up) cho người dùng qua WebSocket.
     */
    public void sendLevelUp(Long userId, LevelUpWsResponse payload) {
        try {
            sendToUser(userId, GamificationEvents.LEVEL_UP, payload);
            log.info("Level up event pushed via websocket userId={} newLevel={}", userId, payload.newLevel());
        } catch (Exception ex) {
            log.warn("Level up event websocket push failed userId={} newLevel={}", userId, payload.newLevel(), ex);
        }
    }
}
