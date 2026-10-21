package com.elingo.vocabulary.dto.request;

import com.elingo.common.annotation.ValidSlug;
import com.elingo.vocabulary.entity.DeckStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record UpdateDeckRequest(
        @NotBlank(message = "INVALID_REQUEST")
        String title,

        @ValidSlug
        String slug,

        String description,

        Boolean isPremium,

        String coverImageUrl,

        DeckStatus status,

        Set<Long> tagIds,

        Set<Long> cefrLevelIds
) {}