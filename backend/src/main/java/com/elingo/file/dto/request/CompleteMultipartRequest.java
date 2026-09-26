package com.elingo.file.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CompleteMultipartRequest(
        @NotBlank(message = "INVALID_REQUEST")
        String fileKey,
        @NotBlank(message = "INVALID_REQUEST")
        String uploadId,
        @NotEmpty(message = "INVALID_REQUEST")
        List<CompletedPartInfo> parts
) {
}
