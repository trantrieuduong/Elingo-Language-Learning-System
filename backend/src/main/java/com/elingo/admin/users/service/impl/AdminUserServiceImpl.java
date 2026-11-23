package com.elingo.admin.users.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.users.dto.request.AdminUserSearchRequest;
import com.elingo.admin.users.dto.request.LockUserRequest;
import com.elingo.admin.users.dto.response.AdminUserResponse;
import com.elingo.admin.users.service.AdminUserService;
import com.elingo.common.dto.PageResponse;
import com.elingo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-USER-SERVICE")
public class AdminUserServiceImpl implements AdminUserService {

    private final UserService userService;

    @Override
    public PageResponse<AdminUserResponse> search(AdminUserSearchRequest request, Pageable pageable) {
        return userService.searchUsersForAdmin(request, pageable);
    }

    @Override
    public AdminUserResponse getById(Long userId) {
        return userService.getById(userId);
    }

    @Override
    @Audited(
            action = AdminAuditAction.USER_LOCK,
            target = AdminAuditTargetType.USER,
            targetId = "#userId.toString()",
            detail = "'Reason: ' + #request.reason"
    )
    public AdminUserResponse lockUser(Long adminId, Long userId, LockUserRequest request) {
        return userService.lockUser(adminId, userId, request.reason());
    }

    @Override
    @Audited(
            action = AdminAuditAction.USER_UNLOCK,
            target = AdminAuditTargetType.USER,
            targetId = "#userId.toString()"
    )
    public AdminUserResponse unlockUser(Long adminId, Long userId) {
        return userService.unlockUser(adminId, userId);
    }
}
