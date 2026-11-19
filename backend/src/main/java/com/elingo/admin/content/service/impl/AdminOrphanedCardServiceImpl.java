package com.elingo.admin.content.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.content.dto.response.OrphanedCardCleanupResponse;
import com.elingo.admin.content.dto.response.OrphanedCardStatsResponse;
import com.elingo.admin.content.service.AdminOrphanedCardService;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-ORPHANED-CARD-SERVICE")
public class AdminOrphanedCardServiceImpl implements AdminOrphanedCardService {

    private final CardService cardService;

    @Override
    public OrphanedCardStatsResponse getStats() {
        return cardService.getOrphanedCardStats();
    }

    @Override
    public PageResponse<CardResponse> getOrphanedCards(Pageable pageable) {
        return cardService.getOrphanedCards(pageable);
    }

    @Override
    @Audited(action = AdminAuditAction.ORPHANED_CARDS_CLEANUP,
            target = AdminAuditTargetType.CARD,
            detail = "'Cleaned up ' + #result.deletedCardCount + ' cards'")
    public OrphanedCardCleanupResponse cleanupOrphanedCards() {
        return cardService.cleanupOrphanedCards();
    }
}
