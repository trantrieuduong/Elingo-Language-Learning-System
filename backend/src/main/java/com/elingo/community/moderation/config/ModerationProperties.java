package com.elingo.community.moderation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.community.moderation")
public record ModerationProperties(
        @DefaultValue("omni-moderation-latest") String openaiModel,
        @DefaultValue("https://api.openai.com") String openaiBaseUrl,
        @DefaultValue("") String openaiApiKey,
        @DefaultValue Scheduler scheduler,
        @DefaultValue Policy policy,
        @DefaultValue ModerationService moderationService) {

    public record Scheduler(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("30") long fixedDelaySeconds,
            @DefaultValue("5") int batchSize,
            @DefaultValue("3") int maxAttempts) {}

    public record ModerationService(
            @DefaultValue("http://localhost:8000") String baseUrl,
            @DefaultValue("") String token) {}

    public record Policy(
            @DefaultValue("0.7") double autoRejectThreshold,
            @DefaultValue("0.5") double reviewThreshold,
            @DefaultValue("12") int videoMaxFrames) {}
}
