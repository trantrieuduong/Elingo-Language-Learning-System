package com.elingo.vocabulary.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.vocabulary.dto.request.CreateTopicRequest;
import com.elingo.vocabulary.dto.request.UpdateTopicRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.*;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TOPIC-SERVICE")
public class TopicServiceImpl implements TopicService {
    private final CardRepository cardRepository;
    private final TopicRepository topicRepository;
    private final DeckRepository deckRepository;
    private final CardMapper cardMapper;
    private final TopicMapper topicMapper;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserCardStateRepository userCardStateRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> getUnlearnedCardsByTopic(Long userId, Long topicId) {
        Topic topic = topicRepository.findByIdWithDeck(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));

        if (topic.getDeck().getIsPremium()) {
            boolean hasActiveSubscription = userSubscriptionRepository
                    .existsByUserIdAndStatusAndEndAtAfter(userId, SubscriptionStatus.ACTIVE, LocalDateTime.now());
            if (!hasActiveSubscription) {
                throw new AppException(AppError.DECK_PREMIUM_REQUIRED);
            }
        }

        List<Card> unlearnedCards = cardRepository
                .findAllUnlearnedCardsByTopicIdWithPhonetics(topicId, userId);

        if (unlearnedCards.isEmpty()) {
            log.info("Cards fetched count=0 topicId={}", topicId);
            return List.of();
        }

        List<Long> cardIds = unlearnedCards.stream().map(Card::getId).toList();
        Map<Long, Boolean> starredMap = userCardStateRepository
                .findByUserIdAndCardIdIn(userId, cardIds)
                .stream()
                .collect(Collectors.toMap(
                        ucs -> ucs.getCard().getId(),
                        ucs -> Boolean.TRUE.equals(ucs.getFlagsStarred())
                ));

        List<CardResponse> cards = unlearnedCards.stream()
                .map(card -> cardMapper.toCardResponse(card, starredMap.getOrDefault(card.getId(), false)))
                .toList();

        log.info("Cards fetched count={} topicId={}", cards.size(), topicId);
        return cards;
    }

    @Override
    @Transactional
    public TopicResponse createUserTopic(CreateTopicRequest request, Long currentUserId) {
        Deck deck = deckRepository.findById(request.deckId())
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        if (deck.getOwnerType() != OwnerType.USER || !deck.getOwner().getId().equals(currentUserId)) {
            throw new AppException(AppError.TOPIC_ACCESS_DENIED);
        }

        Topic topic = insertTopicCoreLogic(deck, request);
        return topicMapper.toTopicResponse(topic);
    }

    @Override
    @Transactional
    public TopicResponse createSystemTopic(CreateTopicRequest request) {
        Deck deck = deckRepository.findById(request.deckId())
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        if (deck.getOwnerType() != OwnerType.SYSTEM) {
            throw new AppException(AppError.TOPIC_ACCESS_DENIED);
        }

        Topic topic = insertTopicCoreLogic(deck, request);
        return topicMapper.toTopicResponse(topic);
    }

    private Topic insertTopicCoreLogic(Deck deck, CreateTopicRequest request) {
        if (topicRepository.existsByDeckIdAndSlug(deck.getId(), request.slug())) {
            throw new AppException(AppError.TOPIC_SLUG_EXISTED);
        }

        Topic topic = new Topic();
        topic.setDeck(deck);
        topic.setName(request.name());
        topic.setSlug(request.slug());
        topic.setOrder(topicRepository.findMaxOrderByDeckId(deck.getId()) + 1);
        topic.setCardCount(0);
        topicRepository.save(topic);

        deck.setTopicCount(deck.getTopicCount() + 1);
        deckRepository.save(deck);
        return topic;
    }


    @Override
    @Transactional
    public TopicResponse updateUserTopic(Long topicId, UpdateTopicRequest request, Long currentUserId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));

        Deck deck = topic.getDeck();
        if (deck.getOwnerType() != OwnerType.USER || !deck.getOwner().getId().equals(currentUserId)) {
            throw new AppException(AppError.TOPIC_ACCESS_DENIED);
        }
        return topicMapper.toTopicResponse(updateTopicCoreLogic(topic, request));
    }

    @Override
    @Transactional
    public TopicResponse updateSystemTopic(Long topicId, UpdateTopicRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));

        if (topic.getDeck().getOwnerType() != OwnerType.SYSTEM) {
            throw new AppException(AppError.TOPIC_ACCESS_DENIED);
        }
        return topicMapper.toTopicResponse(updateTopicCoreLogic(topic, request));
    }

    private Topic updateTopicCoreLogic(Topic topic, UpdateTopicRequest request) {
        if (request.name() != null) {
            topic.setName(request.name());
        }

        if (request.slug() != null) {
            if (!topic.getSlug().equals(request.slug()) &&
                    topicRepository.existsByDeckIdAndSlug(topic.getDeck().getId(), request.slug())) {
                throw new AppException(AppError.TOPIC_SLUG_EXISTED);
            }
            topic.setSlug(request.slug());
        }

        Integer requestedOrder = request.order();
        if (requestedOrder != null && !requestedOrder.equals(topic.getOrder())) {
            Integer maxOrder = topicRepository.findMaxOrderByDeckId(topic.getDeck().getId());
            if (requestedOrder < 0 || requestedOrder > maxOrder) {
                throw new AppException(AppError.TOPIC_ORDER_INVALID);
            }

            if (requestedOrder < topic.getOrder()) {
                topicRepository.shiftOrdersDown(
                        topic.getDeck().getId(),
                        requestedOrder,
                        topic.getOrder()
                );
            } else {
                topicRepository.shiftOrdersUp(
                        topic.getDeck().getId(),
                        topic.getOrder(),
                        requestedOrder
                );
            }
            topic.setOrder(requestedOrder);
        }
        return topicRepository.save(topic);
    }

    @Override
    @Transactional
    public void deleteTopic(Long topicId, Long currentUserId, boolean isAdmin) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));
        Deck deck = topic.getDeck();

        if (!isAdmin) {
            if (deck.getOwnerType() != OwnerType.USER || !deck.getOwner().getId().equals(currentUserId)) {
                throw new AppException(AppError.TOPIC_ACCESS_DENIED);
            }
        } else {
            if (deck.getOwnerType() != OwnerType.SYSTEM) {
                throw new AppException(AppError.TOPIC_ACCESS_DENIED);
            }
        }

        int cardsInTopic = topic.getCardCount();
        deck.setTopicCount(deck.getTopicCount() - 1);
        deck.setCardCount(deck.getCardCount() - cardsInTopic);
        deckRepository.save(deck);

        topic.setCardCount(0);
        if (deck.getPublishedAt() != null) {
            topic.setIsDeleted(true);
            topicRepository.save(topic);
            cardRepository.softDeleteByTopicId(topicId);

            log.info("Topic Soft-deleted successfully, topicId={}", topicId);
        } else {
            // Database tự động CASCADE xóa toàn bộ Cards, CardPhonetics
            topicRepository.delete(topic);

            log.info("Topic Hard-deleted successfully, topicId={}", topicId);
        }

        topicRepository.compactOrdersAfterDelete(deck.getId(), topic.getOrder());
    }
}