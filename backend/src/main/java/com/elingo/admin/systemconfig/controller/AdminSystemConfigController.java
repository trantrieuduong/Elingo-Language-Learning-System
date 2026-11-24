package com.elingo.admin.systemconfig.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.systemconfig.dto.request.UpdateSystemConfigRequest;
import com.elingo.admin.systemconfig.dto.response.SystemConfigResponse;
import com.elingo.admin.systemconfig.service.AdminSystemConfigService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.entity.ConfigModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AdminApiPaths.SYSTEM_CONFIGS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - System Config",
        description = "Management endpoints for system configurations in the admin console"
)
public class AdminSystemConfigController {

    private final AdminSystemConfigService adminSystemConfigService;

    @GetMapping
    @Operation(
            summary = "Get list of system configs",
            description = "Retrieves all system configs with optional filtering by module. Sensitive configs with isSecret=true have their values masked with '********'."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "List of system configurations retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetConfigsSuccessResponse",
                            summary = "Sample system config list response",
                            value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "id": 1,
                                          "configKey": "app.premium.bank-transfer.code-prefix",
                                          "configValue": "ELGO",
                                          "valueType": "STRING",
                                          "isSecret": false,
                                          "module": "PREMIUM",
                                          "description": "Prefix code for bank transfer transactions",
                                          "updatedBy": 1,
                                          "updatedAt": "2026-11-19T10:00:00",
                                          "createdAt": "2026-11-19T08:00:00"
                                        },
                                        {
                                          "id": 6,
                                          "configKey": "app.premium.sepay.secret-key",
                                          "configValue": "********",
                                          "valueType": "STRING",
                                          "isSecret": true,
                                          "module": "PREMIUM",
                                          "description": "SePay webhook secret authentication key",
                                          "updatedBy": 1,
                                          "updatedAt": "2026-11-19T10:00:00",
                                          "createdAt": "2026-11-19T08:00:00"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<List<SystemConfigResponse>> getConfigs(
            @Parameter(
                    description = "Optional module filter (e.g., PREMIUM, COMMUNITY)",
                    schema = @Schema(implementation = ConfigModule.class)
            )
            @RequestParam(required = false) ConfigModule module) {
        return ApiResponse.<List<SystemConfigResponse>>builder()
                .success(true)
                .data(adminSystemConfigService.getConfigs(module))
                .build();
    }

    @GetMapping("/config-module")
    @Operation(
            summary = "Get list of config modules",
            description = "Retrieves all available configuration module names for filtering system configurations"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "List of configuration modules retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetConfigModulesSuccessResponse",
                            summary = "Sample module list response",
                            value = """
                                    {
                                      "success": true,
                                      "data": [
                                        "PREMIUM",
                                        "COMMUNITY"
                                      ]
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<List<String>> getConfigModules() {
        return ApiResponse.<List<String>>builder()
                .success(true)
                .data(adminSystemConfigService.getModules())
                .build();
    }

    @PutMapping("/{key}")
    @Operation(
            summary = "Update system config by key",
            description = "Updates the value of a system configuration by its key. Validates value type, encrypts with AES-256-GCM if marked as secret, records an audit log entry, and reloads the in-memory cache."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "System configuration update payload",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = UpdateSystemConfigRequest.class),
                    examples = @ExampleObject(
                            name = "UpdateSystemConfigRequestExample",
                            summary = "Sample update payload",
                            value = """
                                    {
                                      "value": "ELGO_NEW",
                                      "description": "Updated prefix code for transactions"
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "System configuration updated successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UpdateSystemConfigSuccessResponse",
                            summary = "Sample updated config response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "configKey": "app.premium.bank-transfer.code-prefix",
                                        "configValue": "ELGO_NEW",
                                        "valueType": "STRING",
                                        "isSecret": false,
                                        "module": "PREMIUM",
                                        "description": "Updated prefix code for transactions",
                                        "updatedBy": 1,
                                        "updatedAt": "2026-11-19T10:15:00",
                                        "createdAt": "2026-11-19T08:00:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid value format according to valueType"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found - Configuration key does not exist"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<SystemConfigResponse> updateConfig(
            @CurrentUserId Long adminId,
            @Parameter(
                    description = "Configuration key to update",
                    required = true,
                    example = "app.premium.bank-transfer.code-prefix"
            )
            @PathVariable String key,
            @Valid @RequestBody UpdateSystemConfigRequest request) {
        return ApiResponse.<SystemConfigResponse>builder()
                .success(true)
                .data(adminSystemConfigService.updateConfig(key, request, adminId))
                .build();
    }
}
