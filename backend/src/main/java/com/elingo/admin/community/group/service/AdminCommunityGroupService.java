package com.elingo.admin.community.group.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.community.dto.request.AdminPostGroupRequest;
import com.elingo.community.dto.response.AdminPostGroupResponse;
import org.springframework.data.domain.Pageable;

public interface AdminCommunityGroupService {

    PageResponse<AdminPostGroupResponse> search(String keyword, Pageable pageable);

    AdminPostGroupResponse getById(Long groupId);

    AdminPostGroupResponse create(Long adminId, AdminPostGroupRequest request);

    AdminPostGroupResponse update(Long adminId, Long groupId, AdminPostGroupRequest request);

    void delete(Long adminId, Long groupId);
}
