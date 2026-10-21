package com.elingo.vocabulary.dto.request;

import com.elingo.common.annotation.ValidSlug;
import jakarta.validation.constraints.NotBlank;

public record UpdateTopicRequest(
        @NotBlank(message = "INVALID_REQUEST")
        String name,

        @ValidSlug
        String slug,

        Integer order
) {}