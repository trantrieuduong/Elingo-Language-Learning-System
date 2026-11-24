package com.elingo.vocabulary.service.impl;

import com.elingo.vocabulary.dto.request.CreateSystemTopicRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemTopicRequest;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.util.SlugUtils;
import com.elingo.learning.flashcard.dto.response.CardLearningState;
import com.elingo.learning.flashcard.service.CardStateService;
import com.elingo.vocabulary.config.FlashcardProperties;
import com.elingo.vocabulary.dto.request.CreateUserTopicRequest;
import com.elingo.vocabulary.dto.request.UpdateUserTopicRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.DeckAccessPolicy;
import com.elingo.vocabulary.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TOPIC-SERVICE")
public class TopicServiceImpl implements TopicService {

    private final DeckRepository deckRepository;
    private final TopicRepository topicRepository;
    private final DeckAccessPolicy accessPolicy;
    private final CardRepository cardRepository;
    private final CardStateService cardStateService;
    private final CardMapper cardMapper;
    private final FlashcardProperties flashcardProperties;
    private final TopicMapper topicMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> getCardsByTopic(Long topicId, Long userId) {
        DeckRepository.DeckAccessView deck = deckRepository.findAccessViewByTopicId(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));

        accessPolicy.assertCanLearn(deck, userId);

        List<Card> cards = cardRepository.findByTopicIdWithPhonetics(topicId);

        Map<Long, CardLearningState> states = (userId == null || cards.isEmpty())
                ? Map.of()
                : cardStateService.getStates(userId, cards.stream().map(Card::getId).toList());

        return cards.stream().map(card -> cardMapper.toCardResponse(card, userId == null
                ? null
                : states.getOrDefault(card.getId(), CardLearningState.NONE))
        ).toList();
    }

    @Override
    @Transactional
    public TopicResponse createUserTopic(Long userId, CreateUserTopicRequest request) {
        Long deckId = request.deckId();
        assertCanEditDeck(userId, deckId);

        Deck deck = deckRepository.findByIdForUpdate(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        if (topicRepository.countByDeckId(deckId)
                >= flashcardProperties.getUserDeckLimit().getMaxTopicsPerDeck())
            throw new AppException(AppError.TOPIC_LIMIT_EXCEEDED);

        Topic topic = topicRepository.save(Topic.builder()
                .deck(deck)
                .name(request.name())
                .slug(SlugUtils.generateUniqueSlug(request.name()))
                .order(nextOrder(deckId))
                .build());

        deckRepository.adjustTopicCount(deckId, 1);

        log.info("User topic created deckId={} topicId={}", deckId, topic.getId());
        return topicMapper.toTopicResponse(topic);
    }

    @Override
    @Transactional
    public TopicResponse updateUserTopic(Long userId, Long topicId, UpdateUserTopicRequest request) {
        assertCanEditByTopic(userId, topicId);
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));

        topicMapper.update(topic, request);

        log.info("User topic updated topicId={}", topicId);
        return topicMapper.toTopicResponse(topic);
    }

    @Override
    @Transactional
    public void deleteUserTopic(Long userId, Long topicId) {
        assertCanEditByTopic(userId, topicId);
        Topic topic = topicRepository.findByIdForUpdate(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));
        doDeleteTopic(topic);
    }

    // ==========================================
    // Admin Operations
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByDeckIdForAdmin(Long deckId) {
        if (!deckRepository.existsById(deckId)) {
            throw new AppException(AppError.DECK_NOT_FOUND);
        }
        List<Topic> topics = topicRepository.findByDeckId(deckId,
                Sort.by("order").and(Sort.by("id")));
        return topics.stream().map(topicMapper::toTopicResponse).toList();
    }

    @Override
    @Transactional
    public TopicResponse createSystemTopic(CreateSystemTopicRequest request) {
        Long deckId = request.deckId();
        Deck deck = deckRepository.findByIdForUpdate(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        assertSystemDeck(deck);

        Topic topic = topicMapper.toTopic(request);
        topic.setDeck(deck);
        topic.setSlug(SlugUtils.generateUniqueSlug(request.name()));
        topic.setOrder(nextOrder(deckId));
        topic.setCardCount(0);

        Topic saved = topicRepository.save(topic);
        deckRepository.adjustTopicCount(deckId, 1);

        log.info("System topic created deckId={} topicId={}", deckId, saved.getId());
        return topicMapper.toTopicResponse(saved);
    }

    @Override
    @Transactional
    public TopicResponse updateSystemTopic(Long topicId, UpdateSystemTopicRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));

        assertSystemDeck(topic.getDeck());

        topicMapper.updateSystemTopic(topic, request);

        log.info("System topic updated topicId={}", topicId);
        return topicMapper.toTopicResponse(topic);
    }

    @Override
    @Transactional
    public void reorderSystemTopics(Long deckId, List<Long> orderedIds) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));
        assertSystemDeck(deck);

        if (new HashSet<>(orderedIds).size() != orderedIds.size()
                || orderedIds.size() != topicRepository.countByDeckId(deckId)) {
            throw new AppException(AppError.INVALID_REQUEST);
        }
        Map<Long, Topic> byId = topicRepository.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));
        for (int i = 0; i < orderedIds.size(); i++) {
            Topic topic = byId.get(orderedIds.get(i));
            if (topic == null) throw new AppException(AppError.TOPIC_NOT_FOUND);
            if (!topic.getDeck().getId().equals(deckId)) throw new AppException(AppError.TOPIC_NOT_IN_DECK);
            topic.setOrder(i + 1);
        }
        log.info("Topics reordered deckId={} count={}", deckId, orderedIds.size());
    }

    @Override
    @Transactional
    public void deleteSystemTopic(Long topicId) {
        Topic topic = topicRepository.findByIdForUpdate(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));
        assertSystemDeck(topic.getDeck());
        doDeleteTopic(topic);
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private void doDeleteTopic(Topic topic) {
        Long deckId = topic.getDeck().getId();
        Long topicId = topic.getId();

        // 1. Tách thẻ chủ động theo Card Lifecycle Rules
        int detached = cardRepository.detachByTopicId(topicId);

        // 2. Xóa topic
        topicRepository.deleteById(topicId);

        // 3. Đồng bộ biến đếm trên Deck
        deckRepository.adjustTopicCount(deckId, -1);
        if (detached > 0) {
            deckRepository.adjustCardCount(deckId, -detached);
        }

        log.info("Topic deleted topicId={} deckId={} detachedCards={}", topicId, deckId, detached);
    }

    private void assertSystemDeck(Deck deck) {
        if (deck.getOwnerType() != OwnerType.SYSTEM)
            throw new AppException(AppError.DECK_NOT_SYSTEM);
    }

    private void assertCanEditByTopic(Long userId, Long topicId) {
        DeckRepository.DeckAccessView view = deckRepository.findAccessViewByTopicId(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));
        accessPolicy.assertCanEdit(view, userId);
    }

    private void assertCanEditDeck(Long userId, Long deckId) {
        DeckRepository.DeckAccessView view = deckRepository.findAccessViewById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));
        accessPolicy.assertCanEdit(view, userId);
    }

    private int nextOrder(Long deckId) {
        return topicRepository.findFirstByDeckIdOrderByOrderDesc(deckId)
                .map(last -> last.getOrder() + 1)
                .orElse(1);
    }
}
