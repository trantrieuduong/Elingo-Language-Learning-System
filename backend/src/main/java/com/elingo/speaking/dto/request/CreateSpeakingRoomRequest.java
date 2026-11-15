package com.elingo.speaking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreateSpeakingRoomRequest(
        @Size(max = 255, message = "INVALID_REQUEST") 
        String topic,
        @Min(value = 2, message = "INVALID_REQUEST") @Max(value = 4, message = "INVALID_REQUEST") 
        Integer maxParticipants,
        Boolean isPrivate
) {
}
