package com.elingo.community.dto.request;

import com.elingo.common.annotation.AllowedMediaKind;
import com.elingo.common.annotation.BuildMediaKey;
import com.elingo.common.enums.MediaKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreatePostRequest(
        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Long groupId,
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 255, message = "INVALID_REQUEST")
        String title,
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 2000, message = "INVALID_REQUEST")
        String content,
        @BuildMediaKey
        @Size(max = 5, message = "INVALID_REQUEST")
        Set<@NotBlank @AllowedMediaKind({MediaKind.IMAGE, MediaKind.VIDEO}) String> mediaUrls
) {
    public CreatePostRequest {
        title = title == null ? null : title.strip();
        content = content == null ? null : content.strip();
        mediaUrls = mediaUrls == null ? Set.of() : mediaUrls;
    }
}
