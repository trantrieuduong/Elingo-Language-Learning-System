package com.elingo.speaking.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EndSpeakingRoomRequest(
        @NotBlank(message = "INVALID_REQUEST") 
        String roomCode
) {
}
