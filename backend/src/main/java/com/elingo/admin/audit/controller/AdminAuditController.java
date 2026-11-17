package com.elingo.admin.audit.controller;

import com.elingo.admin.audit.dto.request.AdminAuditLogFilterRequest;
import com.elingo.admin.audit.dto.response.AdminAuditLogResponse;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.audit.service.AdminAuditService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    public ApiResponse<PageResponse<AdminAuditLogResponse>> search(
            @Valid
            @ModelAttribute
            AdminAuditLogFilterRequest filter,

            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ApiResponse.<PageResponse<AdminAuditLogResponse>>builder()
                .success(true)
                .data(adminAuditService.search(filter, pageable))
                .build();
    }

    @GetMapping("/actions")
    public ApiResponse<List<String>> getActions() {
        return ApiResponse.<List<String>>builder()
                .success(true)
                .data(Arrays.stream(AdminAuditAction.values()).map(Enum::name).toList())
                .build();
    }

    @GetMapping("/target-types")
    public ApiResponse<List<String>> getTargetTypes() {
        return ApiResponse.<List<String>>builder()
                .success(true)
                .data(Arrays.stream(AdminAuditTargetType.values()).map(Enum::name).toList())
                .build();
    }
}
