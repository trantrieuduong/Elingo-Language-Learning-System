package com.elingo.battle.websocket;

import com.elingo.common.websocket.AbstractWsSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Tập trung mọi lệnh gửi event tới client qua /user/queue/battle.
 * Không nơi nào khác gọi SimpMessagingTemplate trực tiếp cho battle.
 */
@Component
public class BattleEventPublisher extends AbstractWsSender {

    public BattleEventPublisher(SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    @Override
    protected String userQueueDestination() {
        return "/queue/battle";
    }

    @Override
    public String destinationPrefix() {
        return "/app/battle";
    }
}
