package com.elingo.vocabulary.dto.request;

import com.elingo.common.annotation.ValidSlug;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record CreateDeckRequest(
        @NotBlank(message = "INVALID_REQUEST")
        String title,

        @ValidSlug
        String slug,

        String description,

        Boolean isPremium,

        String coverImageUrl,

        Set<Long> tagIds,

        Set<Long> cefrLevelIds
) {}