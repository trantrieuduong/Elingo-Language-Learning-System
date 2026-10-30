package com.elingo.community.dto.request;

import com.elingo.common.annotation.AllowedMediaKind;
import com.elingo.common.annotation.BuildMediaKey;
import com.elingo.common.enums.MediaKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 1000, message = "INVALID_REQUEST")
        String content,

        @BuildMediaKey
        @AllowedMediaKind(MediaKind.IMAGE)
        String imageUrl
) {
    public UpdateCommentRequest {
        content = content == null ? null : content.strip();
        imageUrl = (imageUrl == null || imageUrl.isBlank()) ? null : imageUrl.strip();
    }
}

