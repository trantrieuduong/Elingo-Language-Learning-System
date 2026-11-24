package com.elingo.vocabulary.repository.specification;

import com.elingo.vocabulary.dto.request.AdminDeckSearchRequest;
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
        Set<Long> tagIds,
        Set<Long> cefrLevelIds
) {
    public static DeckFilterBuilder fromRequest(DeckSearchRequest request) {
        return builder()
                .keyword(request.q())
                .tagIds(request.tagIds())
                .cefrLevelIds(request.cefrLevelIds())
                .premium(request.premium())
                .ownerType(request.ownerType());
    }

    public static DeckFilter fromAdminRequest(AdminDeckSearchRequest request) {
        return builder()
                .keyword(request.keyword())
                .ownerType(request.ownerType())
                .statuses(request.status() != null ? Set.of(request.status()) : null)
                .premium(request.isPremium())
                .tagIds(request.tagIds())
                .cefrLevelIds(request.cefrLevelIds())
                .build();
    }
}
