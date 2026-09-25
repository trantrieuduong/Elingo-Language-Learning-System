package com.elingo.learning.flashcard.service;

import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;

public interface FlashcardService {
    void submitSrsReview(Long userId, Long cardId, SrsReviewRequest request);

    void toggleStar(Long userId, Long cardId);

    void toggleHide(Long userId, Long cardId);
}
