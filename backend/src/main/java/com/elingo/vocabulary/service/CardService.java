package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.request.CreateUserCardRequest;
import com.elingo.vocabulary.dto.request.UpdateUserCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;

public interface CardService {
    CardResponse createUserCard(Long userId, CreateUserCardRequest request);
    CardResponse updateUserCard(Long userId, Long cardId, UpdateUserCardRequest request);
    void deleteUserCard(Long userId, Long cardId);
}
