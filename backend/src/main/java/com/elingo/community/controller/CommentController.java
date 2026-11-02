package com.elingo.community.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.enums.RateLimitRule;
import com.elingo.common.service.RateLimiterService;
import com.elingo.community.dto.request.CreateCommentRequest;
import com.elingo.community.dto.request.UpdateCommentRequest;
import com.elingo.community.dto.response.CommentResponse;
import com.elingo.community.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Tag(name = "Comment Controller")
public class CommentController {

    private final CommentService commentService;
    private final RateLimiterService limiter;

    @PostMapping
    @Operation(summary = "Create comment or reply")
    public ApiResponse<CommentResponse> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @CurrentUserId Long currentUserId) {
        limiter.check(RateLimitRule.COMMENT_POST, String.valueOf(currentUserId));

        return ApiResponse.<CommentResponse>builder()
                .success(true)
                .data(commentService.createComment(request, currentUserId))
                .build();
    }

    @GetMapping("/{commentId}/replies")
    @Operation(summary = "Get replies of a parent comment with pagination")
    public ApiResponse<PageResponse<CommentResponse>> getRepliesByComment(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long commentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<PageResponse<CommentResponse>>builder()
                .success(true)
                .data(commentService.getRepliesByComment(commentId, pageable, currentUserId))
                .build();
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Update comment by ID")
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<CommentResponse>builder()
                .success(true)
                .data(commentService.updateComment(commentId, request, currentUserId))
                .build();
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete comment by ID")
    public ApiResponse<Void> deleteComment(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long commentId,
            @CurrentUserId Long currentUserId) {
        commentService.deleteComment(commentId, currentUserId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping("/{commentId}/like")
    @Operation(summary = "Like a comment")
    public ApiResponse<Void> likeComment(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long commentId,
            @CurrentUserId Long currentUserId) {
        commentService.likeComment(commentId, currentUserId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @DeleteMapping("/{commentId}/like")
    @Operation(summary = "Unlike a comment")
    public ApiResponse<Void> unlikeComment(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long commentId,
            @CurrentUserId Long currentUserId) {
        commentService.unlikeComment(commentId, currentUserId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
