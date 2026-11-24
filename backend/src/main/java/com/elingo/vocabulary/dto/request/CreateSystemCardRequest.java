package com.elingo.vocabulary.dto.request;

import com.elingo.common.annotation.AllowedMediaKind;
import com.elingo.common.annotation.BuildMediaKey;
import com.elingo.common.enums.MediaKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateSystemCardRequest(
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

        String explanationVi,

        String explanationEn,

        String examplesVi,

        String examplesEn,

        @Size(max = 500, message = "INVALID_REQUEST")
        @AllowedMediaKind(MediaKind.IMAGE)
        @BuildMediaKey String imageUrl,

        @Valid
        List<CardPhoneticRequest> phonetics
) {
    public CreateSystemCardRequest {
        term = term == null ? null : term.strip();
        pos = (pos == null || pos.isBlank()) ? null : pos.strip().toLowerCase();
        translation = translation == null ? null : translation.strip();
        explanationVi = (explanationVi == null || explanationVi.isBlank()) ? null : explanationVi.strip();
        explanationEn = (explanationEn == null || explanationEn.isBlank()) ? null : explanationEn.strip();
        examplesVi = (examplesVi == null || examplesVi.isBlank()) ? null : examplesVi.strip();
        examplesEn = (examplesEn == null || examplesEn.isBlank()) ? null : examplesEn.strip();
        imageUrl = (imageUrl == null || imageUrl.isBlank()) ? null : imageUrl.strip();
        phonetics = phonetics == null ? List.of() : List.copyOf(phonetics);
    }
}
