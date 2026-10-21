package com.elingo.vocabulary.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TopicResponse(
        Long id,
        String name,
        String slug,
        Integer order,
        Integer cardCount,
        Integer unlearnedCardCount
) {
}
