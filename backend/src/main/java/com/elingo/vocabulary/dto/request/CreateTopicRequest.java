package com.elingo.vocabulary.dto.request;

import com.elingo.common.annotation.ValidSlug;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTopicRequest(
        @NotNull(message = "INVALID_REQUEST")
        Long deckId,

        @NotBlank(message = "INVALID_REQUEST")
        String name,

        @ValidSlug
        String slug
) {}