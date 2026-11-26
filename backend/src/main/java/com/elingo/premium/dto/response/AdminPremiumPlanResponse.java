package com.elingo.premium.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Premium plan details for administration")
public record AdminPremiumPlanResponse(
        @Schema(description = "Unique identifier of the plan", example = "1")
        Long id,

        @Schema(description = "Display name of the plan", example = "Pro Monthly")
        String name,

        @Schema(description = "Plan price", example = "99000.00")
        BigDecimal price,

        @Schema(description = "Currency code", example = "VND")
        String currency,

        @Schema(description = "Duration in days", example = "30")
        Integer durationDays,

        @Schema(description = "Whether the plan is currently active for purchase", example = "true")
        Boolean isActive,

        @Schema(description = "Creation timestamp", example = "2026-01-01T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp", example = "2026-02-01T12:00:00")
        LocalDateTime updatedAt
) {
}
