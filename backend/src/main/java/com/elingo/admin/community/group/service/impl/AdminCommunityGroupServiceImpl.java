package com.elingo.admin.community.group.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.common.util.AdminPageables;
import com.elingo.admin.community.group.service.AdminCommunityGroupService;
import com.elingo.common.dto.PageResponse;
import com.elingo.community.dto.request.AdminPostGroupRequest;
import com.elingo.community.dto.response.AdminPostGroupResponse;
import com.elingo.community.service.PostGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-COMMUNITY-GROUP-SERVICE")
public class AdminCommunityGroupServiceImpl implements AdminCommunityGroupService {

    private static final Set<String> ALLOWED_SORTS = Set.of("id", "groupName", "createdAt");

    private final PostGroupService postGroupService;

    @Override
    public PageResponse<AdminPostGroupResponse> search(String keyword, Pageable pageable) {
        return postGroupService.searchForAdmin(keyword, AdminPageables.sanitize(pageable, ALLOWED_SORTS));
    }

    @Override
    public AdminPostGroupResponse getById(Long groupId) {
        return postGroupService.getByIdForAdmin(groupId);
    }

    @Override
    @Audited(
            action = AdminAuditAction.POST_GROUP_CREATE,
            target = AdminAuditTargetType.POST_GROUP,
            targetId = "#result.id",
            detail = "'Name: ' + #request.groupName()"
    )
    public AdminPostGroupResponse create(Long adminId, AdminPostGroupRequest request) {
        AdminPostGroupResponse created = postGroupService.createForAdmin(request);
        log.info("Post group created groupId={} byAdminId={}", created.id(), adminId);
        return created;
    }

    @Override
    @Audited(
            action = AdminAuditAction.POST_GROUP_UPDATE,
            target = AdminAuditTargetType.POST_GROUP,
            targetId = "#groupId",
            detail = "'Name: ' + #request.groupName()"
    )
    public AdminPostGroupResponse update(Long adminId, Long groupId, AdminPostGroupRequest request) {
        AdminPostGroupResponse updated = postGroupService.updateForAdmin(groupId, request);
        log.info("Post group updated groupId={} byAdminId={}", groupId, adminId);
        return updated;
    }

    @Override
    @Audited(
            action = AdminAuditAction.POST_GROUP_DELETE,
            target = AdminAuditTargetType.POST_GROUP,
            targetId = "#groupId"
    )
    public void delete(Long adminId, Long groupId) {
        postGroupService.deleteForAdmin(groupId);
        log.info("Post group deleted groupId={} byAdminId={}", groupId, adminId);
    }
}
