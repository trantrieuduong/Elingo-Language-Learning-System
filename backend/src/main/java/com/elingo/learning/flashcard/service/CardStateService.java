package com.elingo.learning.flashcard.service;

import com.elingo.learning.flashcard.dto.response.CardLearningState;

import java.util.Collection;
import java.util.Map;

public interface CardStateService {
    Map<Long, CardLearningState> getStates(Long userId, Collection<Long> cardIds);
}
