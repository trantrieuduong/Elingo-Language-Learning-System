package com.elingo.admin.systemconfig.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.systemconfig.dto.request.UpdateSystemConfigRequest;
import com.elingo.admin.systemconfig.dto.response.SystemConfigResponse;
import com.elingo.admin.systemconfig.service.AdminSystemConfigService;
import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.SystemConfig;
import com.elingo.common.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j(topic = "ADMIN-SYSTEM-CONFIG-SERVICE")
@Service
@RequiredArgsConstructor
public class AdminSystemConfigServiceImpl implements AdminSystemConfigService {

    private static final String MASKED_SECRET = "********";

    private final SystemConfigService systemConfigService;

    @Override
    public List<SystemConfigResponse> getConfigs(ConfigModule module) {
        List<SystemConfig> configs = systemConfigService.getAll(module);
        return configs.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<String> getModules() {
        return Arrays.stream(ConfigModule.values())
                .map(Enum::name)
                .toList();
    }

    @Override
    @Audited(action = AdminAuditAction.SYSTEM_CONFIG_UPDATE,
            target = AdminAuditTargetType.SYSTEM_CONFIG,
            detail = "#configKey")
    public SystemConfigResponse updateConfig(String configKey, UpdateSystemConfigRequest request, Long adminId) {
        SystemConfig updated = systemConfigService.update(configKey, request.value(), request.description(), adminId);
        log.info("System config updated configKey={} byAdminId={}", configKey, adminId);
        return toResponse(updated);
    }

    private SystemConfigResponse toResponse(SystemConfig entity) {
        String displayValue;
        if (Boolean.TRUE.equals(entity.getIsSecret())) {
            String val = entity.getConfigValue();
            displayValue = (val == null || val.isBlank()) ? "" : MASKED_SECRET;
        } else {
            displayValue = entity.getConfigValue() != null ? entity.getConfigValue() : "";
        }
        return SystemConfigResponse.from(entity, displayValue);
    }
}
