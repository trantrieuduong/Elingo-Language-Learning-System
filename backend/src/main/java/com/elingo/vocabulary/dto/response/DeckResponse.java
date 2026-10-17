package com.elingo.vocabulary.dto.response;

import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeckResponse(
        Long id,
        String title,
        String slug,
        String description,
        Boolean isPremium,
        String coverImageUrl,
        DeckStatus status,
        OwnerType ownerType,
        Integer topicCount,
        Integer cardCount,
        Integer learnedCardCount,
        Set<TagResponse> tags,
        Set<CefrLevelResponse> cefrLevels
) {
}