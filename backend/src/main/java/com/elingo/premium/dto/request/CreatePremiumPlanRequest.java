package com.elingo.premium.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request payload for creating a new premium plan")
public record CreatePremiumPlanRequest(
        @Schema(
                description = "Display name of the premium plan",
                example = "Pro Monthly",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 150
        )
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 150, message = "INVALID_REQUEST")
        String name,

        @Schema(
                description = "Price of the premium plan",
                example = "99000.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "INVALID_REQUEST")
        @DecimalMin(value = "0.00", message = "INVALID_REQUEST")
        BigDecimal price,

        @Schema(
                description = "Currency code (ISO 4217)",
                example = "VND",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 10
        )
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 10, message = "INVALID_REQUEST")
        String currency,

        @Schema(
                description = "Validity duration of the plan in days",
                example = "30",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Integer durationDays
) {
}
