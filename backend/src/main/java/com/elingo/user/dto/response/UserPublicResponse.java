package com.elingo.user.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.user.entity.Role;

public record UserPublicResponse(
        Long id,
        String username,
        String fullName,
        @BuildMediaUrl String avatarUrl,
        Role role
) {
}
