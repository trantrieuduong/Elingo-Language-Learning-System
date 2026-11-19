package com.elingo.speaking.dto.response;

public record ChatMessageResponse(
        Long fromUserId,
        String displayName,
        String text,
        long timestamp
) {
}
