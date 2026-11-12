package com.elingo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.thread-pool")
public record ThreadPoolProperties(
        @DefaultValue Scheduler scheduler,
        @DefaultValue Async async
) {

    public record Scheduler(
            @DefaultValue("2") int defaultPoolSize,
            @DefaultValue("2") int battlePoolSize
    ) {}

    public record Async(
            @DefaultValue Email email,
            @DefaultValue Gamification gamification
    ) {}

    public record Email(
            @DefaultValue("1") int corePoolSize,
            @DefaultValue("2") int maxPoolSize,
            @DefaultValue("50") int queueCapacity
    ) {}

    public record Gamification(
            @DefaultValue("1") int corePoolSize,
            @DefaultValue("2") int maxPoolSize,
            @DefaultValue("100") int queueCapacity
    ) {}
}
