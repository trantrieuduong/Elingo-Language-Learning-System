package com.elingo.file.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AbortMultipartRequest(
        @NotBlank(message = "INVALID_REQUEST")
        String fileKey,
        @NotBlank(message = "INVALID_REQUEST")
        String uploadId
) {
}
