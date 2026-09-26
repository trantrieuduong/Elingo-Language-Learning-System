package com.elingo.file.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompletedPartInfo(
        @Positive(message = "INVALID_REQUEST")
        @NotNull(message = "INVALID_REQUEST")
        Integer partNumber,
        @NotBlank(message = "INVALID_REQUEST")
        String etag
) {
}
