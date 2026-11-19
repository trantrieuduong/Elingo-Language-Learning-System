package com.elingo.speaking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.speaking")
public record SpeakingProperties(
        int defaultMaxParticipants,
        int maxParticipantsLimit,
        int signalMaxPayloadBytes,
        List<String> stunUrls,
        long reconnectGraceMs,
        long hostReconnectGraceMs,
        long waitingRoomTtlMs,
        long cleanupIntervalMs
) {
}
