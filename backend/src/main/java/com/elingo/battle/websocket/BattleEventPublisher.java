package com.elingo.battle.websocket;

import com.elingo.battle.dto.response.BattleErrorResponse;
import com.elingo.battle.dto.response.BattleEventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Tập trung mọi lệnh gửi event tới client qua /user/queue/battle.
 * Không nơi nào khác gọi SimpMessagingTemplate trực tiếp cho battle.
 */
@Component
@RequiredArgsConstructor
public class BattleEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi event tới một user cụ thể.
     *
     * @param userId id người nhận
     * @param event  tên event (vd: "match:found", "queue:joined")
     * @param data   payload data (sẽ serialize thành JSON)
     */
    public void sendToUser(Long userId, String event, Object data) {
        BattleEventEnvelope envelope = new BattleEventEnvelope(event, data);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/battle",
                envelope
        );
    }

    // Gửi event error tới một user.
    public void sendError(Long userId, String code, String message) {
        sendToUser(userId, "error", new BattleErrorResponse(code, message));
    }
}
