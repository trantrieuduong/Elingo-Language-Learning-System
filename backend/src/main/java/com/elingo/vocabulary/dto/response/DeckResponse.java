package com.elingo.vocabulary.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeckResponse(
        Long id,
        String title,
        String slug,
        String description,
        Boolean isPremium,
        @BuildMediaUrl String coverImageUrl,
        DeckStatus status,
        OwnerType ownerType,
        Long ownerId,
        Integer topicCount,
        Integer cardCount,
        Integer learnedCardCount,
        LocalDateTime publishedAt,
        Set<TagResponse> tags,
        Set<CefrLevelResponse> cefrLevels
) {
}