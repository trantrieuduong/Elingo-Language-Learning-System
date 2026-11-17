package com.elingo.speaking.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpeakingChatRequest(
        @NotBlank(message = "INVALID_REQUEST") 
        String roomCode,
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 500, message = "INVALID_REQUEST")
        String text
) {
}
