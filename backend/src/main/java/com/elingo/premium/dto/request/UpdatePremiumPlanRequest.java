package com.elingo.premium.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request payload for updating a premium plan (only name and price can be modified)")
public record UpdatePremiumPlanRequest(
        @Schema(
                description = "Display name of the premium plan",
                example = "Pro Monthly (Updated)",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 150
        )
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 150, message = "INVALID_REQUEST")
        String name,

        @Schema(
                description = "Updated price of the premium plan (applies only to future purchases)",
                example = "119000.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "INVALID_REQUEST")
        @DecimalMin(value = "0.00", message = "INVALID_REQUEST")
        BigDecimal price
) {
}
