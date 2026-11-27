package com.elingo.community.service;

import com.elingo.common.dto.CursorResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.community.dto.request.AdminModerationLogFilterRequest;
import com.elingo.community.dto.request.AdminPostModerationSearchRequest;
import com.elingo.community.dto.request.CreatePostRequest;
import com.elingo.community.dto.request.UpdatePostRequest;
import com.elingo.community.dto.response.AdminPostModerationDetailResponse;
import com.elingo.community.dto.response.AdminPostModerationResponse;
import com.elingo.community.dto.response.PostModerationLogResponse;
import com.elingo.community.dto.response.PostResponse;
import org.springframework.data.domain.Pageable;

public interface PostService {

    PostResponse createPost(CreatePostRequest request, Long currentUserId);

    CursorResponse<PostResponse> getPublishedPosts(String cursor, int limit, Long groupId, Long currentUserId);

    CursorResponse<PostResponse> getUserPosts(Long targetUserId, String cursor, int limit, Long currentUserId);

    PostResponse getPostById(Long postId, Long currentUserId);

    PostResponse updatePost(Long postId, UpdatePostRequest request, Long currentUserId);

    void deletePost(Long postId, Long currentUserId);

    void likePost(Long postId, Long currentUserId);

    void unlikePost(Long postId, Long currentUserId);

    // Admin community moderation
    PageResponse<AdminPostModerationResponse> searchPostsForModeration(AdminPostModerationSearchRequest filter, Pageable pageable);

    AdminPostModerationDetailResponse getPostModerationDetail(Long postId);

    PageResponse<PostModerationLogResponse> searchModerationLogs(AdminModerationLogFilterRequest filter, Pageable pageable);

    AdminPostModerationResponse approvePostByAdmin(Long adminId, Long postId, String note);

    AdminPostModerationResponse rejectPostByAdmin(Long adminId, Long postId, String note);
}
