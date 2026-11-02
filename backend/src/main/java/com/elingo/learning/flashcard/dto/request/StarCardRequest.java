package com.elingo.learning.flashcard.dto.request;

import jakarta.validation.constraints.NotNull;

public record StarCardRequest(
        @NotNull(message = "INVALID_REQUEST")
        Long cardId,
        @NotNull(message = "INVALID_REQUEST")
        Boolean starred
) {
}
