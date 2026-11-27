package com.elingo.community.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.community.dto.request.AdminPostGroupRequest;
import com.elingo.community.dto.response.AdminPostGroupResponse;
import com.elingo.community.dto.response.PostGroupResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostGroupService {
    List<PostGroupResponse> getPostGroups();

    // Admin post group management
    PageResponse<AdminPostGroupResponse> searchForAdmin(String keyword, Pageable pageable);

    AdminPostGroupResponse getByIdForAdmin(Long groupId);

    AdminPostGroupResponse createForAdmin(AdminPostGroupRequest request);

    AdminPostGroupResponse updateForAdmin(Long groupId, AdminPostGroupRequest request);

    void deleteForAdmin(Long groupId);
}
