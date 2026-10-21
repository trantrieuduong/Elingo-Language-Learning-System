package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Used for both POST /decks and PUT /decks/{deckId}. */
public record UserDeckRequest(
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 50, message = "INVALID_REQUEST")
        String title,
        @Size(max = 255, message = "INVALID_REQUEST")
        String description
) {
    public UserDeckRequest {
        title = title == null ? null : title.strip();
        description = (description == null || description.isBlank()) ? null : description.strip();
    }
}
