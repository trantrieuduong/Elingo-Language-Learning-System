package com.elingo.admin.content.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.content.dto.request.AdminDeckSearchRequest;
import com.elingo.admin.content.dto.request.SystemDeckRequest;
import com.elingo.admin.content.dto.request.UpdateDeckStatusRequest;
import com.elingo.admin.content.service.AdminDeckService;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.service.DeckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-DECK-SERVICE")
public class AdminDeckServiceImpl implements AdminDeckService {

    private final DeckService deckService;

    @Override
    public PageResponse<DeckResponse> searchDecks(AdminDeckSearchRequest request, Pageable pageable) {
        return deckService.searchDecksForAdmin(request, pageable);
    }

    @Override
    public DeckResponse getDeckById(Long deckId) {
        return deckService.getDeckByIdForAdmin(deckId);
    }

    @Override
    @Audited(action = AdminAuditAction.SYSTEM_DECK_CREATE,
            target = AdminAuditTargetType.DECK,
            targetId = "#result.id",
            detail = "'Title: ' + #request.title")
    public DeckResponse createSystemDeck(SystemDeckRequest request) {
        return deckService.createSystemDeck(request);
    }

    @Override
    @Audited(action = AdminAuditAction.SYSTEM_DECK_UPDATE,
            target = AdminAuditTargetType.DECK,
            targetId = "#deckId",
            detail = "'Update title: ' + #request.title")
    public DeckResponse updateSystemDeck(Long deckId, SystemDeckRequest request) {
        return deckService.updateSystemDeck(deckId, request);
    }

    @Override
    @Audited(action = AdminAuditAction.SYSTEM_DECK_PUBLISH,
            target = AdminAuditTargetType.DECK,
            targetId = "#deckId")
    public DeckResponse publishSystemDeck(Long deckId) {
        return deckService.updateDeckStatus(deckId, new UpdateDeckStatusRequest(DeckStatus.PUBLISHED));
    }

    @Override
    @Audited(action = AdminAuditAction.SYSTEM_DECK_ARCHIVE,
            target = AdminAuditTargetType.DECK,
            targetId = "#deckId")
    public DeckResponse archiveSystemDeck(Long deckId) {
        return deckService.updateDeckStatus(deckId, new UpdateDeckStatusRequest(DeckStatus.ARCHIVED));
    }

    @Override
    public DeckResponse updateStatus(Long deckId, UpdateDeckStatusRequest request) {
        if (request.status() == DeckStatus.PUBLISHED) {
            return publishSystemDeck(deckId);
        } else if (request.status() == DeckStatus.ARCHIVED) {
            return archiveSystemDeck(deckId);
        }
        return deckService.updateDeckStatus(deckId, request);
    }

    @Override
    @Audited(action = AdminAuditAction.USER_DECK_DELETE,
            target = AdminAuditTargetType.DECK,
            targetId = "#deckId",
            detail = "'Reason: ' + #reason")
    public void deleteUserDeck(Long deckId, String reason) {
        deckService.deleteUserDeckByAdmin(deckId, reason);
    }
}
