package com.elingo.vocabulary.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;

public record CardPhoneticResponse(
        String text,
        @BuildMediaUrl String audioUrl,
        String locale
) {
}
