package com.elingo.admin.catalog.controller;

import com.elingo.admin.catalog.dto.request.UpdateSystemConfigRequest;
import com.elingo.admin.catalog.dto.response.SystemConfigResponse;
import com.elingo.admin.catalog.service.AdminCatalogService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.entity.ConfigModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/system-configs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin System Config Controller", description = "Endpoints for managing system configurations in the admin console")
public class AdminSystemConfigController {

    private final AdminCatalogService adminCatalogService;

    @GetMapping
    @Operation(
            summary = "Get list of system configs",
            description = "Retrieves a list of all system configs with optional filtering by module"
    )
    public ApiResponse<List<SystemConfigResponse>> getConfigs(
            @RequestParam(required = false) ConfigModule module) {
        return ApiResponse.<List<SystemConfigResponse>>builder()
                .success(true)
                .data(adminCatalogService.getConfigs(module))
                .build();
    }

    @GetMapping("/config-module")
    @Operation(
            summary = "Get list of config modules",
            description = "Retrieves all available configuration module names"
    )
    public ApiResponse<List<String>> getConfigModules() {
        return ApiResponse.<List<String>>builder()
                .success(true)
                .data(adminCatalogService.getModules())
                .build();
    }

    @PutMapping("/{key}")
    @Operation(
            summary = "Update system config by key",
            description = "Updates the value of a system configuration by its key, validates type, encrypts if secret, and reloads cache"
    )
    public ApiResponse<SystemConfigResponse> updateConfig(
            @PathVariable String key,
            @Valid @RequestBody UpdateSystemConfigRequest request) {
        return ApiResponse.<SystemConfigResponse>builder()
                .success(true)
                .data(adminCatalogService.updateConfig(key, request))
                .build();
    }
}
