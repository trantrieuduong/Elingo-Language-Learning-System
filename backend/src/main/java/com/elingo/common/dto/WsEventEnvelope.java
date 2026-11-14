package com.elingo.common.dto;

/**
 * Envelope chuẩn hóa cho toàn bộ thông điệp WebSocket gửi từ server xuống client.
 *
 * @param event Tên sự kiện (ví dụ: "match:found", "queue:joined", "error")
 * @param data Dữ liệu payload tương ứng với sự kiện
 * @param timestamp Thời điểm tạo thông điệp (Unix epoch milliseconds)
 * @param <T> Kiểu dữ liệu của payload
 */
public record WsEventEnvelope<T>(
        String event,
        T data,
        long timestamp
) {
    public WsEventEnvelope(String event, T data) {
        this(event, data, System.currentTimeMillis());
    }

    public static <T> WsEventEnvelope<T> of(String event, T data) {
        return new WsEventEnvelope<>(event, data);
    }

    public static WsEventEnvelope<Void> of(String event) {
        return new WsEventEnvelope<>(event, null);
    }
}
