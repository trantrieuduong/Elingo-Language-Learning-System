package com.elingo.community.service;

import com.elingo.common.dto.CursorResponse;
import com.elingo.community.dto.request.CreatePostRequest;
import com.elingo.community.dto.request.UpdatePostRequest;
import com.elingo.community.dto.response.PostResponse;

public interface PostService {

    PostResponse createPost(CreatePostRequest request, Long currentUserId);

    CursorResponse<PostResponse> getPublishedPosts(String cursor, int limit, Long groupId, Long currentUserId);

    CursorResponse<PostResponse> getUserPosts(Long targetUserId, String cursor, int limit, Long currentUserId);

    PostResponse getPostById(Long postId, Long currentUserId);

    PostResponse updatePost(Long postId, UpdatePostRequest request, Long currentUserId);

    void deletePost(Long postId, Long currentUserId);

    void likePost(Long postId, Long currentUserId);

    void unlikePost(Long postId, Long currentUserId);
}
