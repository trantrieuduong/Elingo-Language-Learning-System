package com.elingo.learning.lesson.dto.response;

import java.math.BigDecimal;

public record WordAccuracyResponse(
        String word,
        BigDecimal accuracy
) {
}
