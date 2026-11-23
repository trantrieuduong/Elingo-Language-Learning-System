package com.elingo.admin.users.service;

import com.elingo.admin.users.dto.request.AdminUserSearchRequest;
import com.elingo.admin.users.dto.request.LockUserRequest;
import com.elingo.admin.users.dto.response.AdminUserResponse;
import com.elingo.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    PageResponse<AdminUserResponse> search(AdminUserSearchRequest request, Pageable pageable);

    AdminUserResponse getById(Long userId);

    AdminUserResponse lockUser(Long adminId, Long userId, LockUserRequest request);

    AdminUserResponse unlockUser(Long adminId, Long userId);
}
