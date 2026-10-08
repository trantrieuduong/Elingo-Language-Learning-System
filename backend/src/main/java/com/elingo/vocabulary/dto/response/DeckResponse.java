package com.elingo.vocabulary.dto.response;

import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;

import java.util.Set;

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
        Set<TagResponse> tags,
        Set<CefrLevelResponse> cefrLevels
) {
}