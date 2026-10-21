package com.elingo.vocabulary.repository.specification;

import com.elingo.vocabulary.dto.request.DeckSearchRequest;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import lombok.Builder;

import java.util.Set;

@Builder
public record DeckFilter(
        Set<DeckStatus> statuses,
        Long ownerId,
        OwnerType ownerType,
        Boolean premium,
        String keyword,
        Set<String> tags,
        Set<String> cefrLevels
) {
    public static DeckFilterBuilder fromRequest(DeckSearchRequest request) {
        return builder()
                .keyword(request.q())
                .tags(request.tags())
                .cefrLevels(request.cefr())
                .premium(request.premium())
                .ownerType(request.ownerType());
    }
}
