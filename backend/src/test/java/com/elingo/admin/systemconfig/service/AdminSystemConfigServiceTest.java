package com.elingo.admin.systemconfig.service;

import com.elingo.admin.audit.aspect.CurrentAdminProvider;
import com.elingo.admin.systemconfig.dto.request.UpdateSystemConfigRequest;
import com.elingo.admin.systemconfig.dto.response.SystemConfigResponse;
import com.elingo.admin.systemconfig.service.impl.AdminSystemConfigServiceImpl;
import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.ConfigValueType;
import com.elingo.common.entity.SystemConfig;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminSystemConfigServiceTest {

    @Mock
    private SystemConfigService systemConfigService;

    private AdminSystemConfigServiceImpl adminSystemConfigService;

    private static final Long ADMIN_ID = 99L;
    private static final String NON_SECRET_KEY = "app.premium.bank-transfer.code-prefix";
    private static final String SECRET_KEY = "app.premium.sepay.secret-key";
    private static final String DESC = "Prefix code for bank transfer transactions";

    @BeforeEach
    void setUp() {
        adminSystemConfigService = new AdminSystemConfigServiceImpl(systemConfigService);
    }

    // ========================================================================
    // getConfigs Tests
    // ========================================================================
    @Nested
    @DisplayName("getConfigs Tests")
    class GetConfigsTests {

        @Test
        @DisplayName("Should return all configs when module is null")
        void shouldReturnAllConfigsWhenModuleIsNull() {
            SystemConfig config1 = SystemConfig.builder()
                    .id(1L)
                    .configKey(NON_SECRET_KEY)
                    .configValue("ELGO")
                    .valueType(ConfigValueType.STRING)
                    .isSecret(false)
                    .module(ConfigModule.PREMIUM)
                    .description(DESC)
                    .build();

            SystemConfig config2 = SystemConfig.builder()
                    .id(2L)
                    .configKey("app.community.moderation.openai-model")
                    .configValue("omni-moderation-latest")
                    .valueType(ConfigValueType.STRING)
                    .isSecret(false)
                    .module(ConfigModule.COMMUNITY)
                    .description("OpenAI moderation model")
                    .build();

            when(systemConfigService.getAll(null)).thenReturn(List.of(config1, config2));

            List<SystemConfigResponse> result = adminSystemConfigService.getConfigs(null);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).configKey()).isEqualTo(NON_SECRET_KEY);
            assertThat(result.get(0).description()).isEqualTo(DESC);
            assertThat(result.get(1).configKey()).isEqualTo("app.community.moderation.openai-model");
            assertThat(result.get(1).description()).isEqualTo("OpenAI moderation model");
            verify(systemConfigService).getAll(null);
        }

        @Test
        @DisplayName("Should return filtered configs when module parameter is specified")
        void shouldReturnFilteredConfigsWhenModuleIsSpecified() {
            SystemConfig config = SystemConfig.builder()
                    .id(1L)
                    .configKey(NON_SECRET_KEY)
                    .configValue("ELGO")
                    .valueType(ConfigValueType.STRING)
                    .isSecret(false)
                    .module(ConfigModule.PREMIUM)
                    .description(DESC)
                    .build();

            when(systemConfigService.getAll(ConfigModule.PREMIUM)).thenReturn(List.of(config));

            List<SystemConfigResponse> result = adminSystemConfigService.getConfigs(ConfigModule.PREMIUM);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().module()).isEqualTo(ConfigModule.PREMIUM);
            assertThat(result.getFirst().description()).isEqualTo(DESC);
            verify(systemConfigService).getAll(ConfigModule.PREMIUM);
        }

        @Test
        @DisplayName("Should return empty list when no configurations exist")
        void shouldReturnEmptyListWhenNoConfigsFound() {
            when(systemConfigService.getAll(ConfigModule.COMMUNITY)).thenReturn(Collections.emptyList());

            List<SystemConfigResponse> result = adminSystemConfigService.getConfigs(ConfigModule.COMMUNITY);

            assertThat(result).isEmpty();
            verify(systemConfigService).getAll(ConfigModule.COMMUNITY);
        }

        @Test
        @DisplayName("Should mask config value with '********' when isSecret is true and value is present")
        void shouldMaskConfigValueWhenConfigIsSecret() {
            SystemConfig secretConfig = SystemConfig.builder()
                    .id(10L)
                    .configKey(SECRET_KEY)
                    .configValue("v1:EncryptedSecretData123")
                    .valueType(ConfigValueType.STRING)
                    .isSecret(true)
                    .module(ConfigModule.PREMIUM)
                    .description("SePay secret key")
                    .build();

            when(systemConfigService.getAll(ConfigModule.PREMIUM)).thenReturn(List.of(secretConfig));

            List<SystemConfigResponse> result = adminSystemConfigService.getConfigs(ConfigModule.PREMIUM);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().configValue()).isEqualTo("********");
            assertThat(result.getFirst().isSecret()).isTrue();
            assertThat(result.getFirst().description()).isEqualTo("SePay secret key");
        }

        @Test
        @DisplayName("Should return empty string for secret config when stored value is blank or empty")
        void shouldNotMaskConfigValueWhenSecretIsBlankOrNull() {
            SystemConfig emptySecret = SystemConfig.builder()
                    .id(11L)
                    .configKey(SECRET_KEY)
                    .configValue("")
                    .valueType(ConfigValueType.STRING)
                    .isSecret(true)
                    .module(ConfigModule.PREMIUM)
                    .build();

            when(systemConfigService.getAll(ConfigModule.PREMIUM)).thenReturn(List.of(emptySecret));

            List<SystemConfigResponse> result = adminSystemConfigService.getConfigs(ConfigModule.PREMIUM);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().configValue()).isEmpty();
            assertThat(result.getFirst().isSecret()).isTrue();
        }

        @Test
        @DisplayName("Should retain plaintext config value when isSecret is false")
        void shouldRetainPlaintextConfigValueWhenConfigIsNotSecret() {
            SystemConfig plainConfig = SystemConfig.builder()
                    .id(1L)
                    .configKey(NON_SECRET_KEY)
                    .configValue("ELGO_CODE")
                    .valueType(ConfigValueType.STRING)
                    .isSecret(false)
                    .module(ConfigModule.PREMIUM)
                    .description(DESC)
                    .build();

            when(systemConfigService.getAll(ConfigModule.PREMIUM)).thenReturn(List.of(plainConfig));

            List<SystemConfigResponse> result = adminSystemConfigService.getConfigs(ConfigModule.PREMIUM);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().configValue()).isEqualTo("ELGO_CODE");
            assertThat(result.getFirst().isSecret()).isFalse();
            assertThat(result.getFirst().description()).isEqualTo(DESC);
        }

        @Test
        @DisplayName("Should handle null config value for non-secret config by returning empty string")
        void shouldHandleNullConfigValueGracefully() {
            SystemConfig nullValueConfig = SystemConfig.builder()
                    .id(1L)
                    .configKey(NON_SECRET_KEY)
                    .configValue(null)
                    .valueType(ConfigValueType.STRING)
                    .isSecret(false)
                    .module(ConfigModule.PREMIUM)
                    .build();

            when(systemConfigService.getAll(ConfigModule.PREMIUM)).thenReturn(List.of(nullValueConfig));

            List<SystemConfigResponse> result = adminSystemConfigService.getConfigs(ConfigModule.PREMIUM);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().configValue()).isEmpty();
        }
    }

    // ========================================================================
    // getModules Tests
    // ========================================================================
    @Nested
    @DisplayName("getModules Tests")
    class GetModulesTests {

        @Test
        @DisplayName("Should return all module names from ConfigModule enum")
        void shouldReturnAllModuleNames() {
            List<String> modules = adminSystemConfigService.getModules();

            assertThat(modules).containsExactlyInAnyOrder("PREMIUM", "COMMUNITY", "GAMIFICATION");
        }
    }

    // ========================================================================
    // updateConfig Tests
    // ========================================================================
    @Nested
    @DisplayName("updateConfig Tests")
    class UpdateConfigTests {

        @Test
        @DisplayName("Should update non-secret configuration successfully and return plain value with description")
        void shouldUpdateNonSecretConfigSuccessfully() {
            SystemConfig updatedConfig = SystemConfig.builder()
                    .id(1L)
                    .configKey(NON_SECRET_KEY)
                    .configValue("NEW_PREFIX")
                    .valueType(ConfigValueType.STRING)
                    .isSecret(false)
                    .module(ConfigModule.PREMIUM)
                    .description("Updated description")
                    .updatedBy(ADMIN_ID)
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(systemConfigService.update(NON_SECRET_KEY, "NEW_PREFIX", "Updated description", ADMIN_ID))
                    .thenReturn(updatedConfig);

            UpdateSystemConfigRequest request = new UpdateSystemConfigRequest("NEW_PREFIX", "Updated description");
            SystemConfigResponse response = adminSystemConfigService.updateConfig(NON_SECRET_KEY, request, ADMIN_ID);

            assertThat(response).isNotNull();
            assertThat(response.configKey()).isEqualTo(NON_SECRET_KEY);
            assertThat(response.configValue()).isEqualTo("NEW_PREFIX");
            assertThat(response.description()).isEqualTo("Updated description");
            assertThat(response.updatedBy()).isEqualTo(ADMIN_ID);
            verify(systemConfigService).update(NON_SECRET_KEY, "NEW_PREFIX", "Updated description", ADMIN_ID);
        }

        @Test
        @DisplayName("Should update secret configuration successfully and return masked value")
        void shouldUpdateSecretConfigSuccessfullyAndReturnMaskedValue() {
            SystemConfig updatedSecret = SystemConfig.builder()
                    .id(2L)
                    .configKey(SECRET_KEY)
                    .configValue("v1:NewlyEncryptedValue789")
                    .valueType(ConfigValueType.STRING)
                    .isSecret(true)
                    .module(ConfigModule.PREMIUM)
                    .description("Secret description")
                    .updatedBy(ADMIN_ID)
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(systemConfigService.update(SECRET_KEY, "my-new-secret-key", null, ADMIN_ID))
                    .thenReturn(updatedSecret);

            UpdateSystemConfigRequest request = new UpdateSystemConfigRequest("my-new-secret-key", null);
            SystemConfigResponse response = adminSystemConfigService.updateConfig(SECRET_KEY, request, ADMIN_ID);

            assertThat(response).isNotNull();
            assertThat(response.configKey()).isEqualTo(SECRET_KEY);
            assertThat(response.configValue()).isEqualTo("********");
            assertThat(response.isSecret()).isTrue();
            assertThat(response.description()).isEqualTo("Secret description");
            assertThat(response.updatedBy()).isEqualTo(ADMIN_ID);
            verify(systemConfigService).update(SECRET_KEY, "my-new-secret-key", null, ADMIN_ID);
        }

        @Test
        @DisplayName("Should throw AppException when config key is not found")
        void shouldThrowAppExceptionWhenConfigNotFound() {
            when(systemConfigService.update("invalid.key", "some-value", null, ADMIN_ID))
                    .thenThrow(new AppException(AppError.SYSTEM_CONFIG_NOT_FOUND));

            UpdateSystemConfigRequest request = new UpdateSystemConfigRequest("some-value", null);

            assertThatThrownBy(() -> adminSystemConfigService.updateConfig("invalid.key", request, ADMIN_ID))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.SYSTEM_CONFIG_NOT_FOUND);

            verify(systemConfigService).update("invalid.key", "some-value", null, ADMIN_ID);
        }

        @Test
        @DisplayName("Should throw AppException when value format validation fails")
        void shouldThrowAppExceptionWhenValueValidationFails() {
            when(systemConfigService.update("some.int.key", "not-a-number", null, ADMIN_ID))
                    .thenThrow(new AppException(AppError.SYSTEM_CONFIG_VALUE_INVALID));

            UpdateSystemConfigRequest request = new UpdateSystemConfigRequest("not-a-number", null);

            assertThatThrownBy(() -> adminSystemConfigService.updateConfig("some.int.key", request, ADMIN_ID))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.SYSTEM_CONFIG_VALUE_INVALID);

            verify(systemConfigService).update("some.int.key", "not-a-number", null, ADMIN_ID);
        }

    }
}
