package com.elingo.admin.gamification.controller;

import com.elingo.admin.gamification.dto.request.BadgeRequest;
import com.elingo.admin.gamification.dto.response.AdminBadgeResponse;
import com.elingo.admin.gamification.service.AdminBadgeService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.gamification.entity.BadgeConditionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/gamification/badges")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin Badge Controller",
        description = "Management endpoints for badges in the admin console"
)
public class AdminBadgeController {

    private final AdminBadgeService adminBadgeService;

    @GetMapping
    @Operation(summary = "Get all badges", description = "Retrieves all badges, optionally filtered by condition type")
    public ApiResponse<List<AdminBadgeResponse>> getAllBadges(
            @RequestParam(required = false) BadgeConditionType conditionType) {
        return ApiResponse.<List<AdminBadgeResponse>>builder()
                .success(true)
                .data(adminBadgeService.getAllBadges(conditionType))
                .build();
    }

    @GetMapping("/condition-types")
    @Operation(summary = "Get all badge condition types", description = "Retrieves all supported badge condition types for filtering and dropdown options")
    public ApiResponse<List<BadgeConditionType>> getConditionTypes() {
        return ApiResponse.<List<BadgeConditionType>>builder()
                .success(true)
                .data(adminBadgeService.getConditionTypes())
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get badge by ID", description = "Retrieves a specific badge by its ID")
    public ApiResponse<AdminBadgeResponse> getBadgeById(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long id) {
        return ApiResponse.<AdminBadgeResponse>builder()
                .success(true)
                .data(adminBadgeService.getBadgeById(id))
                .build();
    }

    @PostMapping
    @Operation(summary = "Create badge", description = "Creates a new badge with unique code")
    public ApiResponse<AdminBadgeResponse> createBadge(
            @Valid @RequestBody BadgeRequest request) {
        return ApiResponse.<AdminBadgeResponse>builder()
                .success(true)
                .data(adminBadgeService.createBadge(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update badge", description = "Updates an existing badge")
    public ApiResponse<AdminBadgeResponse> updateBadge(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long id,
            @Valid @RequestBody BadgeRequest request) {
        return ApiResponse.<AdminBadgeResponse>builder()
                .success(true)
                .data(adminBadgeService.updateBadge(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete badge", description = "Deletes a badge if it has not been earned by any user")
    public ApiResponse<Void> deleteBadge(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long id) {
        adminBadgeService.deleteBadge(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
