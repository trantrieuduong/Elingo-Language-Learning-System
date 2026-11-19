package com.elingo.speaking.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;

import java.time.LocalDateTime;

public record SpeakingBannedUserResponse(
        Long userId,
        String displayName,
        @BuildMediaUrl String avatarUrl,
        LocalDateTime kickedAt
) {
}
