package com.elingo.vocabulary.dto.request;

import com.elingo.vocabulary.entity.OwnerType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record DeckSearchRequest(
        @Size(max = 100, message = "INVALID_REQUEST")
        String q,
        @Size(max = 10, message = "INVALID_REQUEST")
        Set<@Positive(message = "INVALID_REQUEST") Long> tagIds,
        @Size(max = 6, message = "INVALID_REQUEST")
        Set<@Positive(message = "INVALID_REQUEST") Long> cefrLevelIds,
        Boolean premium,
        OwnerType ownerType
) {
        public DeckSearchRequest {
                if (ownerType == null) ownerType = OwnerType.SYSTEM;
        }
}
