package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.request.CreateCardRequest;
import com.elingo.vocabulary.dto.request.UpdateCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;

public interface CardService {
    CardResponse createUserCard(CreateCardRequest request, Long currentUserId);

    CardResponse createSystemCard(CreateCardRequest request);

    CardResponse updateUserCard(Long cardId, UpdateCardRequest request, Long currentUserId);

    CardResponse updateSystemCard(Long cardId, UpdateCardRequest request);

    void deleteCard(Long cardId, Long currentUserId, boolean isAdmin);
}