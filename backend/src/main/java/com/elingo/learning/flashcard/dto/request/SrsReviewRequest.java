package com.elingo.learning.flashcard.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SrsReviewRequest(
        @NotNull(message = "INVALID_REQUEST")
        @Min(value = 0, message = "INVALID_SRS_GRADE")
        @Max(value = 3, message = "INVALID_SRS_GRADE")
        Integer grade
) {
}