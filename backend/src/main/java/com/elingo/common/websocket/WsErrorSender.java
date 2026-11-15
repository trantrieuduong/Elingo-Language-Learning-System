package com.elingo.common.websocket;

public interface WsErrorSender {
    default String destinationPrefix() {
        return null;
    }
    void sendError(Long userId, String code, String message);
}

