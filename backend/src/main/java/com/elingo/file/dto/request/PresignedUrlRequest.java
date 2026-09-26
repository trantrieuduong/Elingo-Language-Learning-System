package com.elingo.file.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PresignedUrlRequest(
        @NotBlank(message = "INVALID_REQUEST")
        String fileName,
        @NotBlank(message = "INVALID_REQUEST")
        String fileType,
        @Positive(message = "INVALID_REQUEST")
        @NotNull(message = "INVALID_REQUEST")
        Long fileSize // byte
) {
}
