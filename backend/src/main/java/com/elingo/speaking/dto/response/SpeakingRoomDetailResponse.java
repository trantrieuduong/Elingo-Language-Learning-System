package com.elingo.speaking.dto.response;

import com.elingo.speaking.entity.SpeakingRoomStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SpeakingRoomDetailResponse(
        String roomCode,
        String topic,
        Short maxParticipants,
        SpeakingRoomStatus status,
        boolean isPrivate,
        Long hostId,
        LocalDateTime startedAt,
        LocalDateTime createdAt,
        List<SpeakingPeerResponse> participants
) {
}
