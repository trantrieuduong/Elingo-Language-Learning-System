package com.elingo.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Getter
@Setter// Khi app khởi động, Spring đọc YAML rồi gọi setter để inject giá trị vào từng field.
@Validated// Kiểm tra dữ liệu hợp lệ cho các cấu hình được đọc từ file application.yml
@ConfigurationProperties(prefix = "app.flashcard")
public class FlashcardProperties {

    @Valid// Spring tiếp tục vào bên trong class con Srs
    private Srs srs = new Srs();

    @Getter
    @Setter
    public static class Srs {

        @NotNull
        private BigDecimal efMin;

        @NotNull
        private BigDecimal againEfPenalty;

        @NotNull
        private BigDecimal hardEfPenalty;

        @NotNull
        private BigDecimal easyEfBonus;

        @NotNull
        private BigDecimal hardIntervalFactor;

        @NotNull
        private BigDecimal easyIntervalFactor;

        @Min(1)
        private int againReviewMinutes;
    }
}