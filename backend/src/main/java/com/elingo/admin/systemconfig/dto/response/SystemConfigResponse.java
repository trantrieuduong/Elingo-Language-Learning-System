package com.elingo.admin.systemconfig.dto.response;

import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.ConfigValueType;
import com.elingo.common.entity.SystemConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "Response containing detailed system configuration information")
public record SystemConfigResponse(
        @Schema(description = "Unique identifier of the configuration", example = "1")
        Long id,

        @Schema(description = "Unique key identifying the configuration setting", example = "app.premium.bank-transfer.code-prefix")
        String configKey,

        @Schema(description = "Current configuration value (masked with '********' if isSecret is true)", example = "ELGO")
        String configValue,

        @Schema(description = "Data type of the configuration value", example = "STRING")
        ConfigValueType valueType,

        @Schema(description = "Indicates whether this configuration contains sensitive/secret data", example = "false")
        Boolean isSecret,

        @Schema(description = "Module or domain this configuration belongs to", example = "PREMIUM")
        ConfigModule module,

        @Schema(description = "Description or purpose of this configuration setting", example = "Prefix code for bank transfer transactions")
        String description,

        @Schema(description = "Admin user ID who last updated this configuration", example = "1")
        Long updatedBy,

        @Schema(description = "Timestamp when this configuration was last updated", example = "2026-11-19T10:00:00")
        LocalDateTime updatedAt,

        @Schema(description = "Timestamp when this configuration was created", example = "2026-11-19T08:00:00")
        LocalDateTime createdAt
) {
    public static SystemConfigResponse from(SystemConfig entity, String maskedValue) {
        return SystemConfigResponse.builder()
                .id(entity.getId())
                .configKey(entity.getConfigKey())
                .configValue(maskedValue)
                .valueType(entity.getValueType())
                .isSecret(entity.getIsSecret())
                .module(entity.getModule())
                .description(entity.getDescription())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
