package com.elingo.progress.service;

import java.util.Collection;
import java.util.Map;

public interface ProgressService {
    Map<Long, Integer> getLearnedCardCountByDeck(Long userId, Collection<Long> deckIds);
    Map<Long, Integer> getLearnedCardCountByTopic(Long userId, Collection<Long> topicIds);
}
