package com.elingo.learning.flashcard.dto.response;

import java.util.List;

public record QuizResponse(
        List<QuizOptionResponse> options
) {
}