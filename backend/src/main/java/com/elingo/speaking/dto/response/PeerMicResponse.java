package com.elingo.speaking.dto.response;

public record PeerMicResponse(
        Long userId,
        boolean muted
) {
}
