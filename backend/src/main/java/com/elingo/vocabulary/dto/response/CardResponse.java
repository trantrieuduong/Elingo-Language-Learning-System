package com.elingo.vocabulary.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.learning.flashcard.dto.response.CardLearningState;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
        @BuildMediaUrl String imageUrl,
        List<CardPhoneticResponse> phonetics,
        CardLearningState state
) {
}
