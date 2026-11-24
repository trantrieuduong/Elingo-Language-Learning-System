package com.elingo.admin.content.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.vocabulary.dto.request.CreateSystemCardRequest;
import com.elingo.vocabulary.dto.request.ReorderItemRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemCardRequest;
import com.elingo.admin.content.service.AdminCardService;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-CARD-SERVICE")
public class AdminCardServiceImpl implements AdminCardService {

    private final CardService cardService;

    @Override
    public List<CardResponse> getCardsByTopicId(Long topicId) {
        return cardService.getCardsByTopicIdForAdmin(topicId);
    }

    @Override
    public CardResponse getCardById(Long cardId) {
        return cardService.getCardByIdForAdmin(cardId);
    }

    @Override
    @Audited(action = AdminAuditAction.CARD_CREATE,
            target = AdminAuditTargetType.CARD,
            targetId = "#result.id",
            detail = "'Topic: ' + #request.topicId + ', Term: ' + #request.term")
    public CardResponse createSystemCard(CreateSystemCardRequest request) {
        CardResponse created = cardService.createSystemCard(request);
        log.info("System card created cardId={}", created.id());
        return created;
    }

    @Override
    @Audited(action = AdminAuditAction.CARD_UPDATE,
            target = AdminAuditTargetType.CARD,
            targetId = "#cardId",
            detail = "'Update term: ' + #request.term")
    public CardResponse updateSystemCard(Long cardId, UpdateSystemCardRequest request) {
        CardResponse updated = cardService.updateSystemCard(cardId, request);
        log.info("System card updated cardId={}", cardId);
        return updated;
    }

    @Override
    @Audited(action = AdminAuditAction.TOPIC_UPDATE,
            target = AdminAuditTargetType.TOPIC,
            targetId = "#topicId",
            detail = "'Reordered cards'")
    public void reorderSystemCards(Long topicId, ReorderItemRequest request) {
        cardService.reorderSystemCards(topicId, request.orderedIds());
        log.info("System cards reordered topicId={}", topicId);
    }

    @Override
    @Audited(action = AdminAuditAction.CARD_DELETE,
            target = AdminAuditTargetType.CARD,
            targetId = "#cardId")
    public void deleteSystemCard(Long cardId) {
        cardService.deleteSystemCard(cardId);
        log.info("System card deleted cardId={}", cardId);
    }
}
