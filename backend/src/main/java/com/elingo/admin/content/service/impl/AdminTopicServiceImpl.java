package com.elingo.admin.content.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.vocabulary.dto.request.CreateSystemTopicRequest;
import com.elingo.vocabulary.dto.request.ReorderItemRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemTopicRequest;
import com.elingo.admin.content.service.AdminTopicService;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-TOPIC-SERVICE")
public class AdminTopicServiceImpl implements AdminTopicService {

    private final TopicService topicService;

    @Override
    public List<TopicResponse> getTopicsByDeckId(Long deckId) {
        return topicService.getTopicsByDeckIdForAdmin(deckId);
    }

    @Override
    @Audited(action = AdminAuditAction.TOPIC_CREATE,
            target = AdminAuditTargetType.TOPIC,
            targetId = "#result.id",
            detail = "'Deck: ' + #request.deckId + ', Name: ' + #request.name")
    public TopicResponse createSystemTopic(CreateSystemTopicRequest request) {
        TopicResponse created = topicService.createSystemTopic(request);
        log.info("System topic created topicId={}", created.id());
        return created;
    }

    @Override
    @Audited(action = AdminAuditAction.TOPIC_UPDATE,
            target = AdminAuditTargetType.TOPIC,
            targetId = "#topicId",
            detail = "'Update name: ' + #request.name")
    public TopicResponse updateSystemTopic(Long topicId, UpdateSystemTopicRequest request) {
        TopicResponse updated = topicService.updateSystemTopic(topicId, request);
        log.info("System topic updated topicId={}", topicId);
        return updated;
    }

    @Override
    @Audited(action = AdminAuditAction.TOPIC_UPDATE,
            target = AdminAuditTargetType.DECK,
            targetId = "#deckId",
            detail = "'Reordered topics'")
    public void reorderSystemTopics(Long deckId, ReorderItemRequest request) {
        topicService.reorderSystemTopics(deckId, request.orderedIds());
        log.info("System topics reordered deckId={}", deckId);
    }

    @Override
    @Audited(action = AdminAuditAction.TOPIC_DELETE,
            target = AdminAuditTargetType.TOPIC,
            targetId = "#topicId")
    public void deleteSystemTopic(Long topicId) {
        topicService.deleteSystemTopic(topicId);
        log.info("System topic deleted topicId={}", topicId);
    }
}
