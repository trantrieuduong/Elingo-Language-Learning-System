package com.elingo.admin.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for updating a system configuration")
public record UpdateSystemConfigRequest(
        @Schema(
                description = "New configuration value. Pass '********' to keep existing secret value unchanged.",
                example = "ELGO",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Config value must not be null")
        String value
) {}
