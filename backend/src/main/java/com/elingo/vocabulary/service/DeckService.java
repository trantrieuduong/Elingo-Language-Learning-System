package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;

import java.util.List;

public interface DeckService {
    List<TopicResponse> getTopicsByDeck(Long userId, Long deckId);

    List<DeckResponse> getAllPublishedDecks(Long userId);
}