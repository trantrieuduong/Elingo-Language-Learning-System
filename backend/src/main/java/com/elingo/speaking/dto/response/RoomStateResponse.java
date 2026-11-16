package com.elingo.speaking.dto.response;

import java.util.List;

public record RoomStateResponse(
        String roomCode,
        String topic,
        Long hostId,
        Short maxParticipants,
        Long startedAtMs,
        long serverNowMs,
        List<SpeakingPeerResponse> peers
) {
}
