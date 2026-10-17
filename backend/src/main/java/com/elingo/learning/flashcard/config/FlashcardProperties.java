package com.elingo.learning.flashcard.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated// Kiểm tra dữ liệu hợp lệ cho các cấu hình được đọc từ file application.yml
@ConfigurationProperties(prefix = "app.flashcard")
public record FlashcardProperties(
        @Valid @NotNull Srs srs
) {
    public record Srs(
            @NotNull BigDecimal efMin,
            @NotNull BigDecimal againEfPenalty,
            @NotNull BigDecimal hardEfPenalty,
            @NotNull BigDecimal easyEfBonus,
            @NotNull BigDecimal hardIntervalFactor,
            @NotNull BigDecimal easyIntervalFactor,
            @Min(1) int againReviewMinutes
    ) {
    }
}