package com.elingo.file.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyUploadRequest(
        @NotBlank(message = "INVALID_REQUEST")
        String fileKey, // key tạm trong staging/, lấy từ response của bước presign
        @NotBlank(message = "INVALID_REQUEST")
        String fileType // Content-Type client khai, để so với định dạng thật
) {
}
