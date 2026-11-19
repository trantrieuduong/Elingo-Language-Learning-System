package com.elingo.admin.catalog.controller;

import com.elingo.admin.catalog.dto.request.UpdateSystemConfigRequest;
import com.elingo.admin.catalog.dto.response.SystemConfigResponse;
import com.elingo.admin.catalog.service.AdminCatalogService;
import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.ConfigValueType;
import com.elingo.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(adminSystemConfigController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /admin/system-configs should return config list")
    void getConfigs_Success() throws Exception {
        SystemConfigResponse item = SystemConfigResponse.builder()
                .id(1L)
                .configKey("app.premium.bank-transfer.code-prefix")
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
                .andExpect(jsonPath("$.data[0].configKey").value("app.premium.bank-transfer.code-prefix"))
                .andExpect(jsonPath("$.data[0].configValue").value("ELGO"));

        verify(adminCatalogService).getConfigs(ConfigModule.PREMIUM);
    }

    @Test
    @DisplayName("GET /admin/system-configs/config-module should return module names")
    void getConfigModules_Success() throws Exception {
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
    @DisplayName("PUT /admin/system-configs/{key} should update config and return updated response")
    void updateConfig_Success() throws Exception {
        String key = "app.premium.bank-transfer.code-prefix";
        UpdateSystemConfigRequest request = new UpdateSystemConfigRequest("NEW_PREFIX");

        SystemConfigResponse response = SystemConfigResponse.builder()
                .id(1L)
                .configKey(key)
                .configValue("NEW_PREFIX")
                .valueType(ConfigValueType.STRING)
                .isSecret(false)
                .module(ConfigModule.PREMIUM)
                .updatedBy(99L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(adminCatalogService.updateConfig(eq(key), any(UpdateSystemConfigRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/admin/system-configs/{key}", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.configKey").value(key))
                .andExpect(jsonPath("$.data.configValue").value("NEW_PREFIX"));

        verify(adminCatalogService).updateConfig(eq(key), any(UpdateSystemConfigRequest.class));
    }
}
