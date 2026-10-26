package com.elingo.vocabulary.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.request.DeckSearchRequest;
import com.elingo.vocabulary.dto.request.UserDeckRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DeckService {
    PageResponse<DeckResponse> getPublishedDecks(DeckSearchRequest req, Pageable pageable, Long userId);
    DeckResponse getDeckById(Long deckId, Long userId);
    List<TopicResponse> getTopicsByDeckId(Long deckId, Long userId);
    List<TopicResponse> getTopicsByDeckSlug(String slug, Long userId);

    // CRUD user decks
    List<DeckResponse> getUserDecks(Long userId);
    DeckResponse createUserDeck(Long userId, UserDeckRequest request);
    DeckResponse updateUserDeck(Long userId, Long deckId, UserDeckRequest request);
    DeckResponse setUserDeckPublished(Long userId, Long deckId, boolean published);
    void deleteUserDeck(Long userId, Long deckId);
}