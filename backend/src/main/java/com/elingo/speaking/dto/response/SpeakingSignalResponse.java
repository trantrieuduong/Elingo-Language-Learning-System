package com.elingo.speaking.dto.response;

import com.elingo.speaking.dto.request.SpeakingSignalType;
import tools.jackson.databind.JsonNode;

public record SpeakingSignalResponse(
        Long fromUserId,
        SpeakingSignalType type,
        JsonNode payload
) {
}
