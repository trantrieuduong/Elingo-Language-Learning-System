package com.elingo.speaking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.speaking")
public record SpeakingProperties(
        int defaultMaxParticipants,
        int maxParticipantsLimit
) {
}
