package com.elingo.admin.audit.controller;

import com.elingo.admin.audit.dto.request.AdminAuditLogFilterRequest;
import com.elingo.admin.audit.dto.response.AdminAuditLogResponse;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.audit.service.AdminAuditService;
import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(AdminApiPaths.AUDIT_LOGS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Audit Logs",
        description = "Management endpoints for system audit logs in the admin console"
)
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    @Operation(summary = "Search audit logs", description = "Retrieves paginated audit logs filtered by action, target, admin ID, date range")
    public ApiResponse<PageResponse<AdminAuditLogResponse>> search(
            @Valid
            @ParameterObject
            AdminAuditLogFilterRequest filter,

            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ApiResponse.<PageResponse<AdminAuditLogResponse>>builder()
                .success(true)
                .data(adminAuditService.search(filter, pageable))
                .build();
    }

    @GetMapping("/actions")
    @Operation(summary = "Get audit actions", description = "Retrieves all available audit log action types")
    public ApiResponse<List<String>> getActions() {
        return ApiResponse.<List<String>>builder()
                .success(true)
                .data(Arrays.stream(AdminAuditAction.values()).map(Enum::name).toList())
                .build();
    }

    @GetMapping("/target-types")
    @Operation(summary = "Get audit target types", description = "Retrieves all available audit log target entity types")
    public ApiResponse<List<String>> getTargetTypes() {
        return ApiResponse.<List<String>>builder()
                .success(true)
                .data(Arrays.stream(AdminAuditTargetType.values()).map(Enum::name).toList())
                .build();
    }
}
