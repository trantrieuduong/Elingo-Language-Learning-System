package com.elingo.vocabulary.dto.response;

public record TopicResponse(
        Long id,
        String name,
        String slug,
        Integer order,
        Integer cardCount
) {
}
