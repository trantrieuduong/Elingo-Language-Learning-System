package com.elingo.speaking.dto.response;

import com.elingo.speaking.entity.SpeakingRoomStatus;

import java.time.LocalDateTime;

public record SpeakingRoomResponse(
        String roomCode,
        String topic,
        Short maxParticipants,
        SpeakingRoomStatus status,
        boolean isPrivate,
        long participantCount,
        SpeakingPeerResponse host,
        LocalDateTime startedAt,
        LocalDateTime createdAt
) {
}
