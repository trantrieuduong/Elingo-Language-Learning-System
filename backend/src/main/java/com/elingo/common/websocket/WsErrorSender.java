package com.elingo.common.websocket;

public interface WsErrorSender {
    String destinationPrefix();
    void sendError(Long userId, String code, String message);
}

