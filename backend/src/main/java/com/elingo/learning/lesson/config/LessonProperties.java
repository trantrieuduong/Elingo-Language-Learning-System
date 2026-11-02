package com.elingo.learning.lesson.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.lesson")
public record LessonProperties(
        @DefaultValue("60") int dictationPassScore,
        @DefaultValue("55") int shadowingPassScore,
        @DefaultValue("5") int dictationAttemptPenalty,
        @DefaultValue("10") int shadowingAttemptPenalty
) {
}
