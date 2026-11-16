package com.elingo.speaking.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateSpeakingRoomRequest(
        @Size(max = 255, message = "INVALID_REQUEST")
        String topic,
        @Min(value = 2, message = "INVALID_REQUEST") @Max(value = 4, message = "INVALID_REQUEST")
        Integer maxParticipants,
        Boolean isPrivate
) {
}
