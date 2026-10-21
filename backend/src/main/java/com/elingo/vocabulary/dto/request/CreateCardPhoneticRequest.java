package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCardPhoneticRequest(
        @NotBlank(message = "INVALID_REQUEST")
        String text,

        String audioUrl,

        @NotBlank(message = "INVALID_REQUEST")
        String locale
) {
}