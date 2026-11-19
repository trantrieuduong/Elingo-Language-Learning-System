package com.elingo.speaking.dto.response;

public record PeerDisconnectedResponse(
        Long userId,
        long graceMs
) {
}
