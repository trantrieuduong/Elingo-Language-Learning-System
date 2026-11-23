package com.elingo.admin.users.controller;

import com.elingo.admin.users.dto.request.AdminUserSearchRequest;
import com.elingo.admin.users.dto.request.LockUserRequest;
import com.elingo.admin.users.dto.response.AdminUserResponse;
import com.elingo.admin.users.service.AdminUserService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin User Controller",
        description = "Management endpoints for users in the admin console"
)
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(
            summary = "Search and filter users",
            description = "Retrieves a paginated list of users filtered by keyword, role, verification status, active status, and creation date range."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
            )
    )
    public ApiResponse<PageResponse<AdminUserResponse>> searchUsers(
            @Valid @ModelAttribute AdminUserSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<AdminUserResponse>>builder()
                .success(true)
                .data(adminUserService.search(request, pageable))
                .build();
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Get user details by ID",
            description = "Retrieves complete user information by user ID."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User details retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found"
    )
    public ApiResponse<AdminUserResponse> getUserById(
            @Parameter(description = "Identifier of the user", example = "1", required = true)
            @PathVariable Long userId) {
        return ApiResponse.<AdminUserResponse>builder()
                .success(true)
                .data(adminUserService.getById(userId))
                .build();
    }

    @PutMapping("/{userId}/lock")
    @Operation(
            summary = "Lock user account",
            description = "Deactivates a user account with a mandatory ban reason. Administrators cannot lock themselves or other administrators."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User locked successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad request - Cannot lock self or other admins, or user already inactive"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found"
    )
    public ApiResponse<AdminUserResponse> lockUser(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the user to lock", example = "1", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody LockUserRequest request) {
        return ApiResponse.<AdminUserResponse>builder()
                .success(true)
                .data(adminUserService.lockUser(adminId, userId, request))
                .build();
    }

    @PutMapping("/{userId}/unlock")
    @Operation(
            summary = "Unlock user account",
            description = "Reactivates a locked user account and clears ban reason. Administrators cannot unlock themselves or other administrators."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User unlocked successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad request - Cannot unlock self or other admins, or user already active"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found"
    )
    public ApiResponse<AdminUserResponse> unlockUser(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the user to unlock", example = "1", required = true)
            @PathVariable Long userId) {
        return ApiResponse.<AdminUserResponse>builder()
                .success(true)
                .data(adminUserService.unlockUser(adminId, userId))
                .build();
    }
}
