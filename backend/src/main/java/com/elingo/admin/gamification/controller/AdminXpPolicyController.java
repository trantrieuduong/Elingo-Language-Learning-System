package com.elingo.admin.gamification.controller;

import com.elingo.admin.gamification.dto.request.UpdateXpPolicyRequest;
import com.elingo.admin.gamification.dto.response.AdminXpPolicyResponse;
import com.elingo.admin.gamification.service.AdminXpPolicyService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.gamification.entity.XpSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/gamification/xp-policies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin XP Policy Controller",
        description = "Management endpoints for gamification XP rewards and daily limits in admin console"
)
public class AdminXpPolicyController {

    private final AdminXpPolicyService adminXpPolicyService;

    @GetMapping
    @Operation(summary = "Get all XP policies", description = "Retrieves current XP reward amounts and daily limits for all learning activities")
    public ApiResponse<List<AdminXpPolicyResponse>> getAllPolicies() {
        return ApiResponse.<List<AdminXpPolicyResponse>>builder()
                .success(true)
                .data(adminXpPolicyService.getAllPolicies())
                .build();
    }

    @PutMapping("/{source}")
    @Operation(summary = "Update XP policy", description = "Updates XP reward amount and daily limit for a specific activity source")
    public ApiResponse<AdminXpPolicyResponse> updatePolicy(
            @PathVariable XpSource source,
            @Valid @RequestBody UpdateXpPolicyRequest request) {
        return ApiResponse.<AdminXpPolicyResponse>builder()
                .success(true)
                .data(adminXpPolicyService.updatePolicy(source, request))
                .build();
    }
}
