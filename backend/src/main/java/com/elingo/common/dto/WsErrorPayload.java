package com.elingo.common.dto;

/**
 * Payload chuẩn cho các thông báo lỗi qua WebSocket.
 *
 * @param code Mã lỗi (ví dụ: INVALID_REQUEST, INTERNAL_SERVER_ERROR)
 * @param message Thông báo mô tả lỗi
 */
public record WsErrorPayload(
        String code,
        String message
) {}
