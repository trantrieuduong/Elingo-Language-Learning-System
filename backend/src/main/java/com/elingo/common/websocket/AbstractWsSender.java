package com.elingo.common.websocket;

import com.elingo.common.dto.WsErrorPayload;
import com.elingo.common.dto.WsEventEnvelope;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Lớp cơ sở dùng chung cho các service gửi WebSocket event xuống client.
 */
public abstract class AbstractWsSender implements WsErrorSender {

    protected final SimpMessagingTemplate messagingTemplate;

    protected AbstractWsSender(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Queue đích cho user của module tương ứng (ví dụ: "/queue/battle", "/queue/xp-event").
     */
    protected abstract String userQueueDestination();

    /**
     * Prefix client gửi lên qua @MessageMapping (ví dụ: "/app/battle").
     * Trả về null nếu module chỉ push dữ liệu xuống mà không nhận request từ client.
     */
    @Override
    public String destinationPrefix() {
        return null;
    }

    /**
     * Gửi event kèm payload data tới user cụ thể.
     */
    public <T> void sendToUser(Long userId, String event, T data) {
        WsEventEnvelope<T> envelope = new WsEventEnvelope<>(event, data);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                userQueueDestination(),
                envelope
        );
    }

    /**
     * Gửi event không có payload data tới user cụ thể.
     */
    public void sendToUser(Long userId, String event) {
        sendToUser(userId, event, null);
    }

    /**
     * Gửi event lỗi tới user theo cấu trúc chuẩn.
     */
    @Override
    public void sendError(Long userId, String code, String message) {
        sendToUser(userId, "error", new WsErrorPayload(code, message));
    }
}
