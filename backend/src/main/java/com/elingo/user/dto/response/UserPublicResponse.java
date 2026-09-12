package com.elingo.user.dto.response;

import com.elingo.user.entity.Role;

public record UserPublicResponse(
        Long id,
        String username,
        String fullName,
        String avatarUrl,
        Role role
) {
}
