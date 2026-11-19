package com.elingo.speaking.dto.response;

import java.util.List;

public record RoomStateResponse(
        String roomCode,
        String topic,
        Long hostId,
        Short maxParticipants,
        boolean isPrivate,
        Long startedAtMs,
        long serverNowMs,
        List<SpeakingPeerResponse> peers,
        List<Long> mutedUserIds
) {
}
