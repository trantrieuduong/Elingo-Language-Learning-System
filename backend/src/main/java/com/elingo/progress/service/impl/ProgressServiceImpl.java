package com.elingo.progress.service.impl;

import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private final UserCardStateRepository userCardStateRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getLearnedCardCountByDeck(Long userId, Collection<Long> deckIds) {
        if (deckIds.isEmpty()) return Map.of();
        return toMap(userCardStateRepository.countLearnedByDecks(userId, deckIds));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getLearnedCardCountByTopic(Long userId, Collection<Long> topicIds) {
        if (topicIds.isEmpty()) return Map.of();
        return toMap(userCardStateRepository.countLearnedByTopics(userId, topicIds));
    }

    private Map<Long, Integer> toMap(List<UserCardStateRepository.LearnedCount> rows) {
        return rows.stream().collect(Collectors.toMap(
                UserCardStateRepository.LearnedCount::getRefId,
                r -> Math.toIntExact(r.getLearnedCount()))
        );
    }
}
