package com.elingo.learning.flashcard.dto.request;

import jakarta.validation.constraints.NotNull;

public record HiddenCardRequest(
        @NotNull(message = "INVALID_REQUEST")
        Long cardId,
        @NotNull(message = "INVALID_REQUEST")
        Boolean hidden
) {
}
