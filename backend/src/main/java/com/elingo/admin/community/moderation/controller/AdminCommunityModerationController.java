package com.elingo.admin.community.moderation.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.community.moderation.dto.request.ApprovePostRequest;
import com.elingo.admin.community.moderation.dto.request.RejectPostRequest;
import com.elingo.admin.community.moderation.service.AdminCommunityModerationService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.community.dto.request.AdminModerationLogFilterRequest;
import com.elingo.community.dto.request.AdminPostModerationSearchRequest;
import com.elingo.community.dto.response.AdminPostModerationDetailResponse;
import com.elingo.community.dto.response.AdminPostModerationResponse;
import com.elingo.community.dto.response.PostModerationLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(AdminApiPaths.COMMUNITY_MODERATION)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Community Moderation",
        description = "Management endpoints for reviewing community moderation tasks and manually approving or rejecting posts"
)
public class AdminCommunityModerationController {

    private final AdminCommunityModerationService adminCommunityModerationService;

    @GetMapping("/posts")
    @Operation(
            summary = "Search posts for moderation",
            description = "Retrieves a paginated list of community posts filtered by moderation status (e.g. NEEDS_REVIEW), group, author, title keyword, and creation date range. Allowed sort fields: id, createdAt."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Paginated list of posts retrieved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - INVALID_REQUEST, INVALID_KEYWORD, INVALID_DATE_RANGE, or INVALID_SORT",
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
    public ApiResponse<PageResponse<AdminPostModerationResponse>> searchPosts(
            @Valid @ParameterObject AdminPostModerationSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<AdminPostModerationResponse>>builder()
                .success(true)
                .data(adminCommunityModerationService.searchPosts(filter, pageable))
                .build();
    }

    @GetMapping("/posts/{postId}")
    @Operation(
            summary = "Get post moderation detail",
            description = "Retrieves full post content, media attachments, and complete AI/admin moderation log history."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Post moderation detail retrieved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid postId parameter",
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
            description = "Not Found - POST_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<AdminPostModerationDetailResponse> getPostDetail(
            @Parameter(description = "Identifier of the post", example = "105", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long postId) {
        return ApiResponse.<AdminPostModerationDetailResponse>builder()
                .success(true)
                .data(adminCommunityModerationService.getPostDetail(postId))
                .build();
    }

    @GetMapping("/logs")
    @Operation(
            summary = "Search moderation logs",
            description = "Retrieves a paginated list of AI and manual moderation log entries filtered by postId, actorType, action, aiLabel, and date range. Allowed sort fields: id, createdAt."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Paginated list of moderation logs retrieved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - INVALID_REQUEST, INVALID_DATE_RANGE, or INVALID_SORT",
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
    public ApiResponse<PageResponse<PostModerationLogResponse>> searchModerationLogs(
            @Valid @ParameterObject AdminModerationLogFilterRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<PostModerationLogResponse>>builder()
                .success(true)
                .data(adminCommunityModerationService.searchModerationLogs(filter, pageable))
                .build();
    }

    @PutMapping("/posts/{postId}/approve")
    @Operation(
            summary = "Manually approve a post",
            description = "Publishes a post currently in PENDING or NEEDS_REVIEW status, writes a MANUAL_APPROVE moderation log, publishes PostModeratedEvent, and records audit action POST_APPROVE."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Post approved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - INVALID_REQUEST or POST_ALREADY_MODERATED",
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
            description = "Not Found - POST_NOT_FOUND or USER_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<AdminPostModerationResponse> approvePost(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the post to approve", example = "105", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long postId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Optional approval note",
                    required = false,
                    content = @Content(schema = @Schema(implementation = ApprovePostRequest.class))
            )
            @Valid @RequestBody(required = false) ApprovePostRequest request) {
        return ApiResponse.<AdminPostModerationResponse>builder()
                .success(true)
                .data(adminCommunityModerationService.approvePost(adminId, postId, request))
                .build();
    }

    @PutMapping("/posts/{postId}/reject")
    @Operation(
            summary = "Manually reject a post",
            description = "Rejects a post currently in PENDING or NEEDS_REVIEW status with a mandatory reason, writes a MANUAL_REJECT moderation log, publishes PostModeratedEvent, and records audit action POST_REJECT."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Post rejected successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - INVALID_REQUEST or POST_ALREADY_MODERATED",
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
            description = "Not Found - POST_NOT_FOUND or USER_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<AdminPostModerationResponse> rejectPost(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the post to reject", example = "105", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long postId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Rejection reason",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RejectPostRequest.class))
            )
            @Valid @RequestBody RejectPostRequest request) {
        return ApiResponse.<AdminPostModerationResponse>builder()
                .success(true)
                .data(adminCommunityModerationService.rejectPost(adminId, postId, request))
                .build();
    }
}
