package com.elingo.speaking.dto.response;

public record SpeakingHistorySummaryResponse(
        long totalSeconds,
        long roomCount,
        long sessionCount
) {
}
