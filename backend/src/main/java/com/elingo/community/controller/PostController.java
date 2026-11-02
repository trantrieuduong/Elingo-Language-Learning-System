package com.elingo.community.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.CursorResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.enums.RateLimitRule;
import com.elingo.common.service.RateLimiterService;
import com.elingo.community.dto.request.CreatePostRequest;
import com.elingo.community.dto.request.UpdatePostRequest;
import com.elingo.community.dto.response.CommentResponse;
import com.elingo.community.dto.response.PostResponse;
import com.elingo.community.service.CommentService;
import com.elingo.community.service.PostService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "Post Controller")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final RateLimiterService limiter;

    @PostMapping
    @Operation(summary = "Create post")
    public ApiResponse<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @CurrentUserId Long currentUserId) {
        limiter.check(RateLimitRule.CREATE_POST, String.valueOf(currentUserId));

        return ApiResponse.<PostResponse>builder()
                .success(true)
                .data(postService.createPost(request, currentUserId))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get published posts with cursor-based pagination (lazy loading)")
    public ApiResponse<CursorResponse<PostResponse>> getPublishedPosts(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Long groupId,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<CursorResponse<PostResponse>>builder()
                .success(true)
                .data(postService.getPublishedPosts(cursor, limit, groupId, currentUserId))
                .build();
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get post details by ID")
    public ApiResponse<PostResponse> getPostById(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long postId,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<PostResponse>builder()
                .success(true)
                .data(postService.getPostById(postId, currentUserId))
                .build();
    }

    @PutMapping("/{postId}")
    @Operation(summary = "Update post by ID")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long postId,
            @Valid @RequestBody UpdatePostRequest request,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<PostResponse>builder()
                .success(true)
                .data(postService.updatePost(postId, request, currentUserId))
                .build();
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete post by ID")
    public ApiResponse<Void> deletePost(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long postId,
            @CurrentUserId Long currentUserId) {
        postService.deletePost(postId, currentUserId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @GetMapping("/{postId}/comments")
    @Operation(summary = "Get root comments of a post with pagination")
    public ApiResponse<PageResponse<CommentResponse>> getCommentsByPost(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<PageResponse<CommentResponse>>builder()
                .success(true)
                .data(commentService.getCommentsByPost(postId, pageable, currentUserId))
                .build();
    }

    @PostMapping("/{postId}/like")
    @Operation(summary = "Like a post")
    public ApiResponse<Void> likePost(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long postId,
            @CurrentUserId Long currentUserId) {
        postService.likePost(postId, currentUserId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @DeleteMapping("/{postId}/like")
    @Operation(summary = "Unlike a post")
    public ApiResponse<Void> unlikePost(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long postId,
            @CurrentUserId Long currentUserId) {
        postService.unlikePost(postId, currentUserId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
