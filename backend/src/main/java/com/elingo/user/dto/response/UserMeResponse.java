package com.elingo.user.dto.response;

import com.elingo.user.entity.Role;

import java.time.LocalDateTime;

public record UserMeResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String avatarUrl,
        Role role,
        Boolean isVerified,
        Boolean isActive,
        LocalDateTime passwordChangedAt,
        LocalDateTime usernameChangedAt,
        Boolean hasPassword,
        Boolean linkedGoogle
) {
}
