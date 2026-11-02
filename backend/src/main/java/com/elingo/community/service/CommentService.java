package com.elingo.community.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.community.dto.request.CreateCommentRequest;
import com.elingo.community.dto.request.UpdateCommentRequest;
import com.elingo.community.dto.response.CommentResponse;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    CommentResponse createComment(CreateCommentRequest request, Long currentUserId);

    PageResponse<CommentResponse> getCommentsByPost(Long postId, Pageable pageable, Long currentUserId);

    PageResponse<CommentResponse> getRepliesByComment(Long parentCommentId, Pageable pageable, Long currentUserId);

    CommentResponse updateComment(Long commentId, UpdateCommentRequest request, Long currentUserId);

    void deleteComment(Long commentId, Long currentUserId);

    void likeComment(Long commentId, Long currentUserId);

    void unlikeComment(Long commentId, Long currentUserId);
}
