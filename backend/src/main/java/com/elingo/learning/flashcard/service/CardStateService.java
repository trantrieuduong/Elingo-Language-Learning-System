package com.elingo.learning.flashcard.service;

import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.response.CardLearningState;
import com.elingo.vocabulary.dto.response.CardResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CardStateService {
    void submitReview(Long userId, Long cardId, SrsReviewRequest request);
    Map<Long, CardLearningState> getStates(Long userId, Collection<Long> cardIds);
    void setStarred(Long userId, Long cardId, boolean starred);
    void setHidden(Long userId, Long cardId, boolean hidden);
    List<CardResponse> getDueCards(Long userId, int limit);
}
