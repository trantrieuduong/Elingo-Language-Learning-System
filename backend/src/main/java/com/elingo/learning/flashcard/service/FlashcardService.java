package com.elingo.learning.flashcard.service;

import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.response.QuizResponse;
import com.elingo.learning.flashcard.dto.response.StarCardResponse;
import com.elingo.learning.flashcard.dto.response.HideCardResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;

import java.util.List;

public interface FlashcardService {
    List<TopicResponse> getTopicsByDeck(Long userId, Long deckId);

    List<CardResponse> getCardsByTopic(Long userId, Long topicId);

    QuizResponse getQuizOptions(Long userId, Long topicId, Long cardId);

    void submitSrsReview(Long userId, Long cardId, SrsReviewRequest request);

    StarCardResponse toggleStar(Long userId, Long cardId);

    HideCardResponse toggleHide(Long userId, Long cardId);
}
