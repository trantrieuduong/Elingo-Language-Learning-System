package com.elingo.learning.flashcard.service;

import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.response.ReviewCardResponse;

import java.util.List;

public interface FlashcardService {
    void submitSrsReview(Long userId, Long cardId, SrsReviewRequest request);

    void toggleStar(Long userId, Long cardId);

    void toggleHide(Long userId, Long cardId);

    List<ReviewCardResponse> getCardsForReview(Long userId);
}
