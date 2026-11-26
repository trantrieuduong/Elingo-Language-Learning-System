package com.elingo.progress.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.progress")
public record ProgressProperties(
        int defaultDailyProgressDays,
        int maxDailyProgressDays
) {
}
