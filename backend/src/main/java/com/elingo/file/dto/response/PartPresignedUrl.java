package com.elingo.file.dto.response;

public record PartPresignedUrl(
        Integer partNumber,
        String presignedUrl
) {
}
