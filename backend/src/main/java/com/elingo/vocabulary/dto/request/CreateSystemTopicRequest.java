package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSystemTopicRequest(
        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Long deckId,
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 100, message = "INVALID_REQUEST")
        String name
) {
    public CreateSystemTopicRequest {
        name = name == null ? null : name.strip();
    }
}
