package com.elingo.admin.community.moderation.service;

import com.elingo.admin.community.moderation.dto.request.ApprovePostRequest;
import com.elingo.admin.community.moderation.dto.request.RejectPostRequest;
import com.elingo.common.dto.PageResponse;
import com.elingo.community.dto.request.AdminModerationLogFilterRequest;
import com.elingo.community.dto.request.AdminPostModerationSearchRequest;
import com.elingo.community.dto.response.AdminPostModerationDetailResponse;
import com.elingo.community.dto.response.AdminPostModerationResponse;
import com.elingo.community.dto.response.PostModerationLogResponse;
import org.springframework.data.domain.Pageable;

public interface AdminCommunityModerationService {

    PageResponse<AdminPostModerationResponse> searchPosts(AdminPostModerationSearchRequest filter, Pageable pageable);

    AdminPostModerationDetailResponse getPostDetail(Long postId);

    PageResponse<PostModerationLogResponse> searchModerationLogs(AdminModerationLogFilterRequest filter, Pageable pageable);

    AdminPostModerationResponse approvePost(Long adminId, Long postId, ApprovePostRequest request);

    AdminPostModerationResponse rejectPost(Long adminId, Long postId, RejectPostRequest request);
}
