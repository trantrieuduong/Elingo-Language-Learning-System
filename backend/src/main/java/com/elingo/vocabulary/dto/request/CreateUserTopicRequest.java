package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateUserTopicRequest(
        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Long deckId,

        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 50, message = "INVALID_REQUEST")
        String name
) {
    public CreateUserTopicRequest {
        name = name == null ? null : name.strip();
    }
}
