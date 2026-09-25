package com.elingo.learning.flashcard.dto.response;

public record QuizOptionResponse(
        String term,
        boolean isCorrect
) {
}