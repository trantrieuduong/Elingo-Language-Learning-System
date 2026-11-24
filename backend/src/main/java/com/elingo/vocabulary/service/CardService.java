package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.request.CreateSystemCardRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemCardRequest;
import com.elingo.vocabulary.dto.response.OrphanedCardCleanupResponse;
import com.elingo.vocabulary.dto.response.OrphanedCardStatsResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.request.CreateUserCardRequest;
import com.elingo.vocabulary.dto.request.UpdateUserCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CardService {
    CardResponse createUserCard(Long userId, CreateUserCardRequest request);
    CardResponse updateUserCard(Long userId, Long cardId, UpdateUserCardRequest request);
    void deleteUserCard(Long userId, Long cardId);

    // Admin operations
    List<CardResponse> getCardsByTopicIdForAdmin(Long topicId);
    CardResponse getCardByIdForAdmin(Long cardId);
    CardResponse createSystemCard(CreateSystemCardRequest request);
    CardResponse updateSystemCard(Long cardId, UpdateSystemCardRequest request);
    void reorderSystemCards(Long topicId, List<Long> orderedIds);
    void deleteSystemCard(Long cardId);
    OrphanedCardStatsResponse getOrphanedCardStats();
    PageResponse<CardResponse> getOrphanedCards(Pageable pageable);
    OrphanedCardCleanupResponse cleanupOrphanedCards();
}
