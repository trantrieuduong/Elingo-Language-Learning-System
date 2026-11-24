package com.elingo.admin.content.service;

import com.elingo.vocabulary.dto.response.OrphanedCardCleanupResponse;
import com.elingo.vocabulary.dto.response.OrphanedCardStatsResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
import org.springframework.data.domain.Pageable;

public interface AdminOrphanedCardService {
    OrphanedCardStatsResponse getStats();

    PageResponse<CardResponse> getOrphanedCards(Pageable pageable);

    OrphanedCardCleanupResponse cleanupOrphanedCards();
}
