package com.elingo.admin.catalog.dto.response;

import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.ConfigValueType;
import com.elingo.common.entity.SystemConfig;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SystemConfigResponse(
        Long id,
        String configKey,
        String configValue,
        ConfigValueType valueType,
        Boolean isSecret,
        ConfigModule module,
        Long updatedBy,
        LocalDateTime updatedAt,
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
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
