package com.elingo.admin.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateSystemConfigRequest(
        @Schema(description = "New configuration value", example = "ELGO")
        @NotNull(message = "Config value must not be null")
        String value
) {}
