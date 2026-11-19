package com.elingo.admin.catalog.controller;

import com.elingo.admin.catalog.dto.request.UpdateSystemConfigRequest;
import com.elingo.admin.catalog.dto.response.SystemConfigResponse;
import com.elingo.admin.catalog.service.AdminCatalogService;
import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.ConfigValueType;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.common.exception.HttpExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminSystemConfigControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AdminCatalogService adminCatalogService;

    @InjectMocks
    private AdminSystemConfigController adminSystemConfigController;

    private static final String CONFIG_KEY = "app.premium.bank-transfer.code-prefix";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(adminSystemConfigController)
                .setControllerAdvice(new GlobalExceptionHandler(), new HttpExceptionHandler())
                .build();
    }

    // ========================================================================
    // GET /admin/system-configs Tests
    // ========================================================================
    @Nested
    @DisplayName("GET /admin/system-configs Tests")
    class GetConfigsTests {

        @Test
        @DisplayName("Should return 200 and all configurations when module query param is omitted")
        void shouldReturn200AndAllConfigsWhenModuleOmitted() throws Exception {
            SystemConfigResponse item1 = SystemConfigResponse.builder()
                    .id(1L)
                    .configKey(CONFIG_KEY)
                    .configValue("ELGO")
                    .valueType(ConfigValueType.STRING)
                    .isSecret(false)
                    .module(ConfigModule.PREMIUM)
                    .updatedBy(1L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            SystemConfigResponse item2 = SystemConfigResponse.builder()
                    .id(2L)
                    .configKey("app.community.moderation.openai-model")
                    .configValue("omni-moderation-latest")
                    .valueType(ConfigValueType.STRING)
                    .isSecret(false)
                    .module(ConfigModule.COMMUNITY)
                    .updatedBy(1L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(adminCatalogService.getConfigs(null)).thenReturn(List.of(item1, item2));

            mockMvc.perform(get("/admin/system-configs")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].configKey").value(CONFIG_KEY))
                    .andExpect(jsonPath("$.data[1].configKey").value("app.community.moderation.openai-model"));

            verify(adminCatalogService).getConfigs(null);
        }

        @Test
        @DisplayName("Should return 200 and filtered configurations when valid module param is provided")
        void shouldReturn200AndFilteredConfigsWhenModuleProvided() throws Exception {
            SystemConfigResponse item = SystemConfigResponse.builder()
                    .id(1L)
                    .configKey(CONFIG_KEY)
                    .configValue("ELGO")
                    .valueType(ConfigValueType.STRING)
                    .isSecret(false)
                    .module(ConfigModule.PREMIUM)
                    .updatedBy(1L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(adminCatalogService.getConfigs(ConfigModule.PREMIUM)).thenReturn(List.of(item));

            mockMvc.perform(get("/admin/system-configs")
                            .param("module", "PREMIUM")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].configKey").value(CONFIG_KEY))
                    .andExpect(jsonPath("$.data[0].module").value("PREMIUM"));

            verify(adminCatalogService).getConfigs(ConfigModule.PREMIUM);
        }

        @Test
        @DisplayName("Should return 200 with empty list when no configurations match filter")
        void shouldReturn200WithEmptyListWhenNoConfigsMatch() throws Exception {
            when(adminCatalogService.getConfigs(ConfigModule.COMMUNITY)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/admin/system-configs")
                            .param("module", "COMMUNITY")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(adminCatalogService).getConfigs(ConfigModule.COMMUNITY);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when invalid module param value is provided")
        void shouldReturn400WhenModuleParamIsInvalid() throws Exception {
            mockMvc.perform(get("/admin/system-configs")
                            .param("module", "UNKNOWN_MODULE")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(adminCatalogService);
        }
    }

    // ========================================================================
    // GET /admin/system-configs/config-module Tests
    // ========================================================================
    @Nested
    @DisplayName("GET /admin/system-configs/config-module Tests")
    class GetConfigModulesTests {

        @Test
        @DisplayName("Should return 200 and list of all available module names")
        void shouldReturn200AndListOfModuleNames() throws Exception {
            when(adminCatalogService.getModules()).thenReturn(List.of("PREMIUM", "COMMUNITY"));

            mockMvc.perform(get("/admin/system-configs/config-module")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data", containsInAnyOrder("PREMIUM", "COMMUNITY")));

            verify(adminCatalogService).getModules();
        }

        @Test
        @DisplayName("Should return 500 when service throws unexpected runtime exception")
        void shouldReturn500WhenServiceThrowsUnexpectedException() throws Exception {
            when(adminCatalogService.getModules()).thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/admin/system-configs/config-module")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));

            verify(adminCatalogService).getModules();
        }
    }

    // ========================================================================
    // PUT /admin/system-configs/{key} Tests
    // ========================================================================
    @Nested
    @DisplayName("PUT /admin/system-configs/{key} Tests")
    class UpdateConfigTests {

        @Test
        @DisplayName("Should return 200 and updated config response when payload is valid")
        void shouldReturn200AndUpdatedConfigWhenPayloadIsValid() throws Exception {
            UpdateSystemConfigRequest request = new UpdateSystemConfigRequest("NEW_VALUE");

            SystemConfigResponse response = SystemConfigResponse.builder()
                    .id(1L)
                    .configKey(CONFIG_KEY)
                    .configValue("NEW_VALUE")
                    .valueType(ConfigValueType.STRING)
                    .isSecret(false)
                    .module(ConfigModule.PREMIUM)
                    .updatedBy(99L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(adminCatalogService.updateConfig(eq(CONFIG_KEY), any(UpdateSystemConfigRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(put("/admin/system-configs/{key}", CONFIG_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.configKey").value(CONFIG_KEY))
                    .andExpect(jsonPath("$.data.configValue").value("NEW_VALUE"))
                    .andExpect(jsonPath("$.data.updatedBy").value(99));

            verify(adminCatalogService).updateConfig(eq(CONFIG_KEY), any(UpdateSystemConfigRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when request body has null value")
        void shouldReturn400WhenRequestBodyHasNullValue() throws Exception {
            String jsonWithNullValue = "{\"value\": null}";

            mockMvc.perform(put("/admin/system-configs/{key}", CONFIG_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithNullValue))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(adminCatalogService);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when request body is empty or missing")
        void shouldReturn400WhenRequestBodyIsMissing() throws Exception {
            mockMvc.perform(put("/admin/system-configs/{key}", CONFIG_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(adminCatalogService);
        }

        @Test
        @DisplayName("Should return 404 Not Found when target config key does not exist")
        void shouldReturn404WhenConfigKeyDoesNotExist() throws Exception {
            UpdateSystemConfigRequest request = new UpdateSystemConfigRequest("any-value");

            when(adminCatalogService.updateConfig(eq("nonexistent.key"), any(UpdateSystemConfigRequest.class)))
                    .thenThrow(new AppException(AppError.SYSTEM_CONFIG_NOT_FOUND));

            mockMvc.perform(put("/admin/system-configs/{key}", "nonexistent.key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("SYSTEM_CONFIG_NOT_FOUND"));

            verify(adminCatalogService).updateConfig(eq("nonexistent.key"), any(UpdateSystemConfigRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when config value is invalid according to its value type")
        void shouldReturn400WhenConfigValueIsInvalidForType() throws Exception {
            UpdateSystemConfigRequest request = new UpdateSystemConfigRequest("not-a-valid-int");

            when(adminCatalogService.updateConfig(eq("some.int.key"), any(UpdateSystemConfigRequest.class)))
                    .thenThrow(new AppException(AppError.SYSTEM_CONFIG_VALUE_INVALID));

            mockMvc.perform(put("/admin/system-configs/{key}", "some.int.key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("SYSTEM_CONFIG_VALUE_INVALID"));

            verify(adminCatalogService).updateConfig(eq("some.int.key"), any(UpdateSystemConfigRequest.class));
        }
    }
}
