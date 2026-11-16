package com.elingo.speaking.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnterRoomRequest(
        @NotBlank(message = "SPEAKING_ROOM_NOT_FOUND") 
        String roomCode
) {
}
