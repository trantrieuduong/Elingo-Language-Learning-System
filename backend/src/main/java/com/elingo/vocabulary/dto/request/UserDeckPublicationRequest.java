package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserDeckPublicationRequest(
        @NotNull(message = "INVALID_REQUEST")
        Boolean published
) {
}
