package com.elingo.learning.flashcard.dto.request;

import com.elingo.learning.flashcard.annotation.ValidGrade;

public record SrsReviewRequest(
        @ValidGrade
        Integer grade
) {
}