package com.elingo.learning.shadowing.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WordAccuracyItem(
        @NotBlank(message = "WORDS_ACCURACY_INVALID")
        @Size(max = 100, message = "WORDS_ACCURACY_INVALID")
        String word,

        @NotNull(message = "WORDS_ACCURACY_INVALID")
        @DecimalMin(value = "0", message = "WORDS_ACCURACY_INVALID")
        @DecimalMax(value = "100", message = "WORDS_ACCURACY_INVALID")
        BigDecimal accuracy
) {
}
