package com.elingo.admin.community.group.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.community.group.service.AdminCommunityGroupService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.community.dto.request.AdminPostGroupRequest;
import com.elingo.community.dto.response.AdminPostGroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(AdminApiPaths.COMMUNITY_GROUPS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Community Post Groups",
        description = "Management endpoints for community post groups in the admin console"
)
public class AdminCommunityGroupController {

    private final AdminCommunityGroupService adminCommunityGroupService;

    @GetMapping
    @Operation(
            summary = "Search and filter post groups",
            description = "Retrieves a paginated list of community post groups with post counts, optionally filtered by keyword. Allowed sort fields: id, groupName, createdAt."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Paginated list of post groups retrieved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - INVALID_REQUEST or INVALID_SORT",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<PageResponse<AdminPostGroupResponse>> searchGroups(
            @Parameter(description = "Keyword to search across group name and description", example = "Grammar")
            @RequestParam(required = false) @Size(max = 100, message = "INVALID_REQUEST") String keyword,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<AdminPostGroupResponse>>builder()
                .success(true)
                .data(adminCommunityGroupService.search(keyword, pageable))
                .build();
    }

    @GetMapping("/{groupId}")
    @Operation(
            summary = "Get post group detail",
            description = "Retrieves details of a specific community post group including its total post count."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Post group retrieved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid groupId parameter",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found - POST_GROUP_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<AdminPostGroupResponse> getGroupById(
            @Parameter(description = "Identifier of the post group", example = "1", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long groupId) {
        return ApiResponse.<AdminPostGroupResponse>builder()
                .success(true)
                .data(adminCommunityGroupService.getById(groupId))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new post group",
            description = "Creates a new community post group and records audit action POST_GROUP_CREATE."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Post group created successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - INVALID_REQUEST",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Conflict - POST_GROUP_ALREADY_EXISTS",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<AdminPostGroupResponse> createGroup(
            @CurrentUserId Long adminId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Post group creation payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AdminPostGroupRequest.class))
            )
            @Valid @RequestBody AdminPostGroupRequest request) {
        return ApiResponse.<AdminPostGroupResponse>builder()
                .success(true)
                .data(adminCommunityGroupService.create(adminId, request))
                .build();
    }

    @PutMapping("/{groupId}")
    @Operation(
            summary = "Update a post group",
            description = "Overwrites groupName and description (null clears description) for an existing post group and records audit action POST_GROUP_UPDATE."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Post group updated successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - INVALID_REQUEST",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found - POST_GROUP_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Conflict - POST_GROUP_ALREADY_EXISTS",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<AdminPostGroupResponse> updateGroup(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the post group", example = "1", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long groupId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Post group full replacement payload",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AdminPostGroupRequest.class))
            )
            @Valid @RequestBody AdminPostGroupRequest request) {
        return ApiResponse.<AdminPostGroupResponse>builder()
                .success(true)
                .data(adminCommunityGroupService.update(adminId, groupId, request))
                .build();
    }

    @DeleteMapping("/{groupId}")
    @Operation(
            summary = "Delete a post group",
            description = "Deletes an empty community post group and records audit action POST_GROUP_DELETE. Fails with POST_GROUP_IN_USE if posts still belong to this group."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Post group deleted successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid groupId parameter",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found - POST_GROUP_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Conflict - POST_GROUP_IN_USE",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<Void> deleteGroup(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the post group to delete", example = "1", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long groupId) {
        adminCommunityGroupService.delete(adminId, groupId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
