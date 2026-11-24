package com.elingo.admin.content.service;

import com.elingo.vocabulary.dto.request.AdminDeckSearchRequest;
import com.elingo.vocabulary.dto.request.SystemDeckRequest;
import com.elingo.vocabulary.dto.request.UpdateDeckStatusRequest;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.response.DeckResponse;
import org.springframework.data.domain.Pageable;

public interface AdminDeckService {
    PageResponse<DeckResponse> searchDecks(AdminDeckSearchRequest request, Pageable pageable);

    DeckResponse getDeckById(Long deckId);

    DeckResponse createSystemDeck(SystemDeckRequest request);

    DeckResponse updateSystemDeck(Long deckId, SystemDeckRequest request);

    DeckResponse publishSystemDeck(Long deckId);

    DeckResponse archiveSystemDeck(Long deckId);

    DeckResponse updateStatus(Long deckId, UpdateDeckStatusRequest request);

    void deleteUserDeck(Long deckId, String reason);
}
