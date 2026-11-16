package com.elingo.speaking.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpeakingSignalRequest(
        @NotBlank(message = "SPEAKING_ROOM_NOT_FOUND") 
        String roomCode,
        @NotNull(message = "SPEAKING_SIGNAL_INVALID") 
        Long toUserId,
        @NotNull(message = "SPEAKING_SIGNAL_INVALID") 
        SpeakingSignalType type,
        @NotNull(message = "SPEAKING_SIGNAL_INVALID") 
        JsonNode payload
) {
}
