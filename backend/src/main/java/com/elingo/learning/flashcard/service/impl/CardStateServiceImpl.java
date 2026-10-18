package com.elingo.learning.flashcard.service.impl;

import com.elingo.learning.flashcard.dto.response.CardLearningState;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.learning.flashcard.service.CardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardStateServiceImpl implements CardStateService {

    private final UserCardStateRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, CardLearningState> getStates(Long userId, Collection<Long> cardIds) {
        if (userId == null || cardIds.isEmpty()) return Map.of();
        return repository.findStateViews(userId, cardIds).stream()
                .collect(Collectors.toMap(UserCardStateRepository.CardStateView::getCardId, this::toState));
    }

    private CardLearningState toState(UserCardStateRepository.CardStateView v) {
        return new CardLearningState(
                v.getSrsLastGrade() != null,
                Boolean.TRUE.equals(v.getFlagsStarred()),
                Boolean.TRUE.equals(v.getFlagsHidden())
        );
    }
}
