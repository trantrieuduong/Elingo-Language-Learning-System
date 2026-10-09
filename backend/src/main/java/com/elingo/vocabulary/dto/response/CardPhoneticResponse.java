package com.elingo.vocabulary.dto.response;

public record CardPhoneticResponse(
        String text,
        String audioUrl,
        String locale
) {
}
