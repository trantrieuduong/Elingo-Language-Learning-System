package com.elingo.admin.users.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.user.dto.request.AdminUserSearchRequest;
import com.elingo.admin.users.dto.request.LockUserRequest;
import com.elingo.user.dto.response.AdminUserResponse;
import com.elingo.admin.users.service.AdminUserService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AdminApiPaths.USERS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Users",
        description = "Management endpoints for users in the admin console"
)
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(
            summary = "Search and filter users",
            description = "Retrieves a paginated list of users filtered by keyword (username, email, or full name), role, verification status, active status, and registration date range."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "Paginated list of users retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "SearchUsersSuccessResponse",
                                    summary = "Sample paginated user list response",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "content": [
                                                  {
                                                    "id": 10,
                                                    "username": "john_doe",
                                                    "email": "john.doe@example.com",
                                                    "fullName": "John Doe",
                                                    "avatarUrl": "https://cdn.elingo.com/avatars/user-10.png",
                                                    "role": "USER",
                                                    "isVerified": true,
                                                    "isActive": true,
                                                    "passwordChangedAt": "2026-02-15T08:30:00",
                                                    "usernameChangedAt": "2026-01-10T14:20:00",
                                                    "hasPassword": true,
                                                    "linkedGoogle": false,
                                                    "createdAt": "2026-01-01T10:00:00",
                                                    "updatedAt": "2026-02-15T08:30:00"
                                                  }
                                                ],
                                                "page": 1,
                                                "size": 20,
                                                "totalElements": 1,
                                                "totalPages": 1,
                                                "isLast": true
                                              }
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid query parameters or date range",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "InvalidDateRangeResponse",
                                    summary = "Invalid date range error",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "INVALID_DATE_RANGE",
                                                "message": "Invalid date range or exceeds maximum allowed limit"
                                              }
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
    public ApiResponse<PageResponse<AdminUserResponse>> searchUsers(
            @Valid @ParameterObject AdminUserSearchRequest request,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<AdminUserResponse>>builder()
                .success(true)
                .data(adminUserService.search(request, pageable))
                .build();
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Get user details by ID",
            description = "Retrieves complete user information by user ID for administrative inspection."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "User details retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "GetUserByIdSuccessResponse",
                                    summary = "Sample user details response",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "id": 10,
                                                "username": "john_doe",
                                                "email": "john.doe@example.com",
                                                "fullName": "John Doe",
                                                "avatarUrl": "https://cdn.elingo.com/avatars/user-10.png",
                                                "role": "USER",
                                                "isVerified": true,
                                                "isActive": true,
                                                "passwordChangedAt": "2026-02-15T08:30:00",
                                                "usernameChangedAt": "2026-01-10T14:20:00",
                                                "hasPassword": true,
                                                "linkedGoogle": false,
                                                "createdAt": "2026-01-01T10:00:00",
                                                "updatedAt": "2026-02-15T08:30:00"
                                              }
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Not Found - User does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "UserNotFoundResponse",
                                    summary = "User not found error",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "USER_NOT_FOUND",
                                                "message": "User not found"
                                              }
                                            }
                                            """
                            )
                    )
            )
    public ApiResponse<AdminUserResponse> getUserById(
            @Parameter(description = "Identifier of the user", example = "10", required = true)
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User lock reason payload",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LockUserRequest.class),
                    examples = @ExampleObject(
                            name = "LockUserRequestExample",
                            summary = "Sample lock user request payload",
                            value = """
                                    {
                                      "reason": "Violation of community standards: repetitive spamming"
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User locked successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "LockUserSuccessResponse",
                                    summary = "Sample locked user response",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "id": 10,
                                                "username": "john_doe",
                                                "email": "john.doe@example.com",
                                                "fullName": "John Doe",
                                                "avatarUrl": "https://cdn.elingo.com/avatars/user-10.png",
                                                "role": "USER",
                                                "isVerified": true,
                                                "isActive": false,
                                                "passwordChangedAt": "2026-02-15T08:30:00",
                                                "usernameChangedAt": "2026-01-10T14:20:00",
                                                "hasPassword": true,
                                                "linkedGoogle": false,
                                                "createdAt": "2026-01-01T10:00:00",
                                                "updatedAt": "2026-02-15T08:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Cannot lock self or other admins, invalid reason, or user already inactive",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "CannotModifySelfResponse",
                                            summary = "Self modification rejection",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "error": {
                                                        "code": "CANNOT_MODIFY_SELF",
                                                        "message": "You cannot perform this action on your own account"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "CannotModifyAdminResponse",
                                            summary = "Target is administrator rejection",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "error": {
                                                        "code": "CANNOT_MODIFY_ADMIN",
                                                        "message": "Cannot perform administrative actions on another administrator"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "UserInactiveResponse",
                                            summary = "User already inactive",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "error": {
                                                        "code": "USER_INACTIVE",
                                                        "message": "User account is currently inactive"
                                                      }
                                                    }
                                                    """
                                    )
                            }
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Not Found - User does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "UserNotFoundResponse",
                                    summary = "User not found error",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "USER_NOT_FOUND",
                                                "message": "User not found"
                                              }
                                            }
                                            """
                            )
                    )
            )
    public ApiResponse<AdminUserResponse> lockUser(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the user to lock", example = "10", required = true)
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
            description = "Reactivates a previously locked user account and clears ban reason. Administrators cannot unlock themselves or other administrators."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User unlocked successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UnlockUserSuccessResponse",
                            summary = "Sample unlocked user response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 10,
                                        "username": "john_doe",
                                        "email": "john.doe@example.com",
                                        "fullName": "John Doe",
                                        "avatarUrl": "https://cdn.elingo.com/avatars/user-10.png",
                                        "role": "USER",
                                        "isVerified": true,
                                        "isActive": true,
                                        "passwordChangedAt": "2026-02-15T08:30:00",
                                        "usernameChangedAt": "2026-01-10T14:20:00",
                                        "hasPassword": true,
                                        "linkedGoogle": false,
                                        "createdAt": "2026-01-01T10:00:00",
                                        "updatedAt": "2026-02-15T08:30:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Cannot unlock self or other admins, or user already active",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "CannotModifySelfResponse",
                                    summary = "Self modification rejection",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "CANNOT_MODIFY_SELF",
                                                "message": "You cannot perform this action on your own account"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "CannotModifyAdminResponse",
                                    summary = "Target is administrator rejection",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "CANNOT_MODIFY_ADMIN",
                                                "message": "Cannot perform administrative actions on another administrator"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "UserAlreadyActiveResponse",
                                    summary = "User already active",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "USER_ALREADY_ACTIVE",
                                                "message": "User account is already active"
                                              }
                                            }
                                            """
                            )
                    }
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found - User does not exist",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UserNotFoundResponse",
                            summary = "User not found error",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "USER_NOT_FOUND",
                                        "message": "User not found"
                                      }
                                    }
                                    """
                    )
            )
    )

    public ApiResponse<AdminUserResponse> unlockUser(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the user to unlock", example = "10", required = true)
            @PathVariable Long userId) {
        return ApiResponse.<AdminUserResponse>builder()
                .success(true)
                .data(adminUserService.unlockUser(adminId, userId))
                .build();
    }
}
