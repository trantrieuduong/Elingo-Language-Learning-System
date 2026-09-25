package com.elingo.vocabulary.dto.response;

import java.util.List;

public record CardResponse(
        Long id,
        Integer order,
        String term,
        String pos,
        String translation,
        String explanationVi,
        String explanationEn,
        String examplesVi,
        String examplesEn,
        String imageUrl,
        List<CardPhoneticResponse> phonetics
) {
}
