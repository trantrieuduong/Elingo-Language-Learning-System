package com.elingo.vocabulary.dto.request;

import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AdminDeckSearchRequest(
        @Size(max = 255, message = "KEYWORD_INVALID")
        String keyword,

        OwnerType ownerType,

        DeckStatus status,

        Boolean isPremium,

        Set<Long> tagIds,

        Set<Long> cefrLevelIds
) {
    public AdminDeckSearchRequest {
        keyword = (keyword == null || keyword.isBlank()) ? null : keyword.strip();
    }
}
