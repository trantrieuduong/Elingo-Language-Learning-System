package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateCardRequest(
        Integer order,

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