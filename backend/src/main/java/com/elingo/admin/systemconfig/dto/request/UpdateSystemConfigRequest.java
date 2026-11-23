package com.elingo.admin.systemconfig.dto.request;

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
        String value,

        @Schema(
                description = "Optional updated description for the configuration setting",
                example = "Prefix code for bank transfer transactions"
        )
        String description
) {}
