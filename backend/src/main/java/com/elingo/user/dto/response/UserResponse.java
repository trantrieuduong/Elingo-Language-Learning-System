package com.elingo.user.dto.response;

import com.elingo.user.entity.Role;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String avatarUrl,
        Role role,
        Boolean isVerified,
        Boolean isActive
) {
}
