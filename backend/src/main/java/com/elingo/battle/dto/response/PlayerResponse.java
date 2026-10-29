package com.elingo.battle.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;

public record PlayerResponse(
        Long userId,
        String displayName,
        @BuildMediaUrl String avatarUrl,
        Boolean isHost
) {
}
