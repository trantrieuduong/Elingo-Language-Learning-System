package com.elingo.speaking.dto.response;

public record PeerLeftResponse(
        Long userId,
        String reason
) {
}
