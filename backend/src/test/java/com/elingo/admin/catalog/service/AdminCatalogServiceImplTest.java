package com.elingo.admin.catalog.service;

import com.elingo.admin.audit.aspect.CurrentAdminProvider;
import com.elingo.admin.catalog.dto.request.UpdateSystemConfigRequest;
import com.elingo.admin.catalog.dto.response.SystemConfigResponse;
import com.elingo.admin.catalog.service.impl.AdminCatalogServiceImpl;
import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.ConfigValueType;
import com.elingo.common.entity.SystemConfig;
import com.elingo.common.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCatalogServiceImplTest {

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private CurrentAdminProvider currentAdminProvider;

    private AdminCatalogServiceImpl adminCatalogService;

    @BeforeEach
    void setUp() {
        adminCatalogService = new AdminCatalogServiceImpl(systemConfigService, currentAdminProvider);
    }

    @Test
    @DisplayName("getConfigs should mask secret configs and keep normal configs")
    void getConfigs_MasksSecret() {
        SystemConfig normal = SystemConfig.builder()
                .configKey("app.premium.bank-transfer.code-prefix")
                .configValue("ELGO")
                .valueType(ConfigValueType.STRING)
                .isSecret(false)
                .module(ConfigModule.PREMIUM)
                .build();

        SystemConfig secret = SystemConfig.builder()
                .configKey("app.premium.sepay.secret-key")
                .configValue("v1:encryptedValue")
                .valueType(ConfigValueType.STRING)
                .isSecret(true)
                .module(ConfigModule.PREMIUM)
                .build();

        when(systemConfigService.getAll(ConfigModule.PREMIUM)).thenReturn(List.of(normal, secret));

        List<SystemConfigResponse> result = adminCatalogService.getConfigs(ConfigModule.PREMIUM);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).configValue()).isEqualTo("ELGO");
        assertThat(result.get(0).isSecret()).isFalse();

        assertThat(result.get(1).configValue()).isEqualTo("********");
        assertThat(result.get(1).isSecret()).isTrue();
    }

    @Test
    @DisplayName("getModules should return all enum values")
    void getModules_ReturnsAllModules() {
        List<String> modules = adminCatalogService.getModules();
        assertThat(modules).containsExactlyInAnyOrder("PREMIUM", "COMMUNITY");
    }

    @Test
    @DisplayName("updateConfig should delegate to systemConfigService and return response")
    void updateConfig_Success() {
        when(currentAdminProvider.id()).thenReturn(99L);
        SystemConfig updated = SystemConfig.builder()
                .configKey("app.premium.bank-transfer.code-prefix")
                .configValue("NEW_PREFIX")
                .valueType(ConfigValueType.STRING)
                .isSecret(false)
                .module(ConfigModule.PREMIUM)
                .updatedBy(99L)
                .build();

        when(systemConfigService.update("app.premium.bank-transfer.code-prefix", "NEW_PREFIX", 99L))
                .thenReturn(updated);

        SystemConfigResponse res = adminCatalogService.updateConfig(
                "app.premium.bank-transfer.code-prefix",
                new UpdateSystemConfigRequest("NEW_PREFIX")
        );

        assertThat(res.configKey()).isEqualTo("app.premium.bank-transfer.code-prefix");
        assertThat(res.configValue()).isEqualTo("NEW_PREFIX");
        assertThat(res.updatedBy()).isEqualTo(99L);
    }
}
