package com.elingo.speaking.dto.response;

import com.elingo.speaking.entity.SpeakingRoomStatus;

import java.time.LocalDateTime;

public record SpeakingHistoryItemResponse(
        String roomCode,
        String topic,
        Short maxParticipants,
        String role,
        int visitCount,
        LocalDateTime joinedAt,
        LocalDateTime leftAt,
        long durationSeconds,
        SpeakingRoomStatus roomStatus,
        boolean canRejoin
) {
}
