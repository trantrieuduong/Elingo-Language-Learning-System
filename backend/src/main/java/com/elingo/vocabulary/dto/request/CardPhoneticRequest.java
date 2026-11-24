package com.elingo.vocabulary.dto.request;

import com.elingo.common.annotation.AllowedMediaKind;
import com.elingo.common.annotation.BuildMediaKey;
import com.elingo.common.enums.MediaKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CardPhoneticRequest(
        @Size(max = 255, message = "INVALID_REQUEST")
        String text,

        @Size(max = 500, message = "INVALID_REQUEST")
        @AllowedMediaKind(MediaKind.AUDIO)
        @BuildMediaKey String audioUrl,

        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 10, message = "INVALID_REQUEST")
        String locale
) {
    public CardPhoneticRequest {
        text = (text == null || text.isBlank()) ? null : text.strip();
        audioUrl = (audioUrl == null || audioUrl.isBlank()) ? null : audioUrl.strip();
        locale = locale == null ? null : locale.strip().toLowerCase();
    }
}
