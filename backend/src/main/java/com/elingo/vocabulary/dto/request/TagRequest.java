package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 150, message = "INVALID_REQUEST")
        String label
) {
    public TagRequest {
        label = label == null ? null : label.strip();
    }
}
