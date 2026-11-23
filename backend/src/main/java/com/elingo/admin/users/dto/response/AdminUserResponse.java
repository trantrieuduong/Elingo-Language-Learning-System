package com.elingo.admin.users.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.user.entity.Role;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        @BuildMediaUrl String avatarUrl,
        Role role,
        Boolean isVerified,
        Boolean isActive,
        LocalDateTime passwordChangedAt,
        LocalDateTime usernameChangedAt,
        Boolean hasPassword,
        Boolean linkedGoogle
) {
}
