package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateCardRequest(
        @NotNull(message = "INVALID_REQUEST")
        Long topicId,

        @NotBlank(message = "INVALID_REQUEST")
        String term,

        String pos,
        String translation,
        String explanationVi,
        String explanationEn,
        String examplesVi,
        String examplesEn,
        String imageUrl,
        List<CreateCardPhoneticRequest> phonetics
) {}