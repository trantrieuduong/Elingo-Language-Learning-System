package com.elingo.community.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;

public record AuthorResponse(
        Long id,
        String username,
        String fullName,
        @BuildMediaUrl String avatarUrl
) {
}
