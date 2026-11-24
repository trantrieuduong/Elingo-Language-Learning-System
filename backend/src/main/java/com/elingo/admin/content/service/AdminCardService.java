package com.elingo.admin.content.service;

import com.elingo.vocabulary.dto.request.CreateSystemCardRequest;
import com.elingo.vocabulary.dto.request.ReorderItemRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;

import java.util.List;

public interface AdminCardService {
    List<CardResponse> getCardsByTopicId(Long topicId);

    CardResponse getCardById(Long cardId);

    CardResponse createSystemCard(CreateSystemCardRequest request);

    CardResponse updateSystemCard(Long cardId, UpdateSystemCardRequest request);

    void reorderSystemCards(Long topicId, ReorderItemRequest request);

    void deleteSystemCard(Long cardId);
}
