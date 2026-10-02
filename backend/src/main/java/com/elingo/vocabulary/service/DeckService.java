package com.elingo.vocabulary.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.request.GetPublishedDecksRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;

import java.util.List;

public interface DeckService {
    List<TopicResponse> getTopicsByDeck(Long userId, Long deckId);

    /**
     * Returns a paginated + filtered page of published decks.
     * Page size is fixed at 9.
     */
    PageResponse<DeckResponse> getAllPublishedDecks(Long userId, GetPublishedDecksRequest request);
}