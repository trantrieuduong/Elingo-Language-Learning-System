package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateUserCardRequest(
        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Long topicId,
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 100, message = "INVALID_REQUEST")
        String term,
        @Size(max = 50, message = "INVALID_REQUEST")
        String pos,
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 500, message = "INVALID_REQUEST")
        String translation,
        @Size(max = 255, message = "INVALID_REQUEST")
        String explanationVi,
        @Size(max = 255, message = "INVALID_REQUEST")
        String explanationEn,
        @Size(max = 255, message = "INVALID_REQUEST")
        String examplesVi,
        @Size(max = 255, message = "INVALID_REQUEST")
        String examplesEn
) {
    public CreateUserCardRequest {
        term = term == null ? null : term.strip();
        pos = (pos == null || pos.isBlank()) ? null : pos.strip().toLowerCase();
        translation = translation == null ? null : translation.strip();
        explanationVi = (explanationVi == null || explanationVi.isBlank()) ? null : explanationVi.strip();
        explanationEn = (explanationEn == null || explanationEn.isBlank()) ? null : explanationEn.strip();
        examplesVi = (examplesVi == null || examplesVi.isBlank()) ? null : examplesVi.strip();
        examplesEn = (examplesEn == null || examplesEn.isBlank()) ? null : examplesEn.strip();
    }
}
