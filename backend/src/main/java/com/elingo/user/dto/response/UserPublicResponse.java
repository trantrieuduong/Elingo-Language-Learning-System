package com.elingo.user.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;

public record UserPublicResponse(
        Long id,
        String username,
        String fullName,
        @BuildMediaUrl String avatarUrl
) {
}
