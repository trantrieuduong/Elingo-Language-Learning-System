package com.elingo.learning.flashcard.dto.response;

import com.elingo.vocabulary.dto.response.CardResponse;

import java.time.LocalDateTime;

public record ReviewCardResponse(
        CardResponse card,
        LocalDateTime srsNextReviewAt,
        Boolean flagsStarred,
        Boolean flagsHidden
) {
}