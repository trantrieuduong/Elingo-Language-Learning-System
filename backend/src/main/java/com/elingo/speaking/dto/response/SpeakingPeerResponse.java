package com.elingo.speaking.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.speaking.entity.ParticipantRole;

public record SpeakingPeerResponse(
        Long userId,
        String displayName,
        @BuildMediaUrl String avatarUrl,
        ParticipantRole role
) {
}
