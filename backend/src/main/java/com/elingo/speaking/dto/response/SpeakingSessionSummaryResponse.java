package com.elingo.speaking.dto.response;

public record SpeakingSessionSummaryResponse(
        String roomCode,
        String topic,
        long durationSeconds
) {
}
