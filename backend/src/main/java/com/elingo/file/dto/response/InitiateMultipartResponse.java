package com.elingo.file.dto.response;

import java.util.List;

public record InitiateMultipartResponse(
        String uploadId,
        String key,
        List<PartPresignedUrl> parts,
        Long partSize
) {
}
