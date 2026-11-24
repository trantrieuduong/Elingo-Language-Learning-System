package com.elingo.admin.content.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.common.util.AdminPageables;
import com.elingo.vocabulary.dto.response.OrphanedCardCleanupResponse;
import com.elingo.vocabulary.dto.response.OrphanedCardStatsResponse;
import com.elingo.admin.content.service.AdminOrphanedCardService;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-ORPHANED-CARD-SERVICE")
public class AdminOrphanedCardServiceImpl implements AdminOrphanedCardService {

    private static final Set<String> ALLOWED_SORTS = Set.of("id", "createdAt");

    private final CardService cardService;

    @Override
    public OrphanedCardStatsResponse getStats() {
        return cardService.getOrphanedCardStats();
    }

    @Override
    public PageResponse<CardResponse> getOrphanedCards(Pageable pageable) {
        return cardService.getOrphanedCards(AdminPageables.sanitize(pageable, ALLOWED_SORTS));
    }

    @Override
    @Audited(action = AdminAuditAction.ORPHANED_CARDS_CLEANUP,
            target = AdminAuditTargetType.CARD,
            detail = "'Cleaned up ' + #result.deletedCardCount + ' cards'")
    public OrphanedCardCleanupResponse cleanupOrphanedCards() {
        OrphanedCardCleanupResponse result = cardService.cleanupOrphanedCards();
        log.info("Orphaned cards cleaned up deletedCount={}", result.deletedCardCount());
        return result;
    }
}
