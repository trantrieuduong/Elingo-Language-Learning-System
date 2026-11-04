package com.elingo.premium.dto.response;

import java.math.BigDecimal;

public record PremiumPlanResponse(
        Long id,
        String name,
        BigDecimal price,
        String currency,
        Integer durationDays
) {
}
