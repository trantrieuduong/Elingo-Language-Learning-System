package com.elingo.admin.community.moderation.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.common.util.AdminPageables;
import com.elingo.admin.community.moderation.dto.request.ApprovePostRequest;
import com.elingo.admin.community.moderation.dto.request.RejectPostRequest;
import com.elingo.admin.community.moderation.service.AdminCommunityModerationService;
import com.elingo.common.dto.PageResponse;
import com.elingo.community.dto.request.AdminModerationLogFilterRequest;
import com.elingo.community.dto.request.AdminPostModerationSearchRequest;
import com.elingo.community.dto.response.AdminPostModerationDetailResponse;
import com.elingo.community.dto.response.AdminPostModerationResponse;
import com.elingo.community.dto.response.PostModerationLogResponse;
import com.elingo.community.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-COMMUNITY-MODERATION-SERVICE")
public class AdminCommunityModerationServiceImpl implements AdminCommunityModerationService {

    private static final Set<String> ALLOWED_POST_SORTS = Set.of("id", "createdAt");
    private static final Set<String> ALLOWED_LOG_SORTS = Set.of("id", "createdAt");

    private final PostService postService;

    @Override
    public PageResponse<AdminPostModerationResponse> searchPosts(
            AdminPostModerationSearchRequest filter,
            Pageable pageable) {
        return postService.searchPostsForModeration(filter, AdminPageables.sanitize(pageable, ALLOWED_POST_SORTS));
    }

    @Override
    public AdminPostModerationDetailResponse getPostDetail(Long postId) {
        return postService.getPostModerationDetail(postId);
    }

    @Override
    public PageResponse<PostModerationLogResponse> searchModerationLogs(
            AdminModerationLogFilterRequest filter,
            Pageable pageable) {
        return postService.searchModerationLogs(filter, AdminPageables.sanitize(pageable, ALLOWED_LOG_SORTS));
    }

    @Override
    @Audited(
            action = AdminAuditAction.POST_APPROVE,
            target = AdminAuditTargetType.POST,
            targetId = "#postId",
            detail = "#request != null ? #request.note() : null"
    )
    public AdminPostModerationResponse approvePost(Long adminId, Long postId, ApprovePostRequest request) {
        String note = request != null ? request.note() : null;
        AdminPostModerationResponse approved = postService.approvePostByAdmin(adminId, postId, note);
        log.info("Post approved postId={} byAdminId={}", postId, adminId);
        return approved;
    }

    @Override
    @Audited(
            action = AdminAuditAction.POST_REJECT,
            target = AdminAuditTargetType.POST,
            targetId = "#postId",
            detail = "#request.note()"
    )
    public AdminPostModerationResponse rejectPost(Long adminId, Long postId, RejectPostRequest request) {
        AdminPostModerationResponse rejected = postService.rejectPostByAdmin(adminId, postId, request.note());
        log.info("Post rejected postId={} byAdminId={}", postId, adminId);
        return rejected;
    }
}
