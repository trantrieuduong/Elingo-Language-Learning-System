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
    List<CardPhoneticResponse> phonetics,
    Boolean flagsStarred
) {
    public CardResponse(
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
        this(id, order, term, pos, translation, explanationVi, explanationEn, examplesVi, examplesEn, imageUrl, phonetics, false);
    }

    public CardResponse withFlagsStarred(Boolean flagsStarred) {
        return new CardResponse(
            id(), order(), term(), pos(), translation(),
            explanationVi(), explanationEn(), examplesVi(), examplesEn(),
            imageUrl(), phonetics(), flagsStarred
        );
    }
}
