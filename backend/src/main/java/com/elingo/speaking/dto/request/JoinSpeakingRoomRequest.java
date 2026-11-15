package com.elingo.speaking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JoinSpeakingRoomRequest(
        @NotBlank(message = "SPEAKING_ROOM_NOT_FOUND") 
        String roomCode
) {
}
