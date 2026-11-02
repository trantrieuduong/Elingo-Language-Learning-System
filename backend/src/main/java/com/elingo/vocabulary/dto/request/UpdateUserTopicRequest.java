package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserTopicRequest(
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 50, message = "INVALID_REQUEST")
        String name
) {
    public UpdateUserTopicRequest {
        name = name == null ? null : name.strip();
    }
}
