package com.elingo.speaking.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UnbanMemberRequest(
        @NotBlank(message = "INVALID_REQUEST") 
        String roomCode,
        @NotNull(message = "INVALID_REQUEST") 
        Long userId
) {
}
