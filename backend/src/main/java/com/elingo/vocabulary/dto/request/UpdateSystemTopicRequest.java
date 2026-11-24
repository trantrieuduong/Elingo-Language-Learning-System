package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSystemTopicRequest(
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 100, message = "INVALID_REQUEST")
        String name
) {
    public UpdateSystemTopicRequest {
        name = name == null ? null : name.strip();
    }
}
