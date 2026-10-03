package com.elingo.vocabulary.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TOPIC-SERVICE")
public class TopicServiceImpl implements TopicService {
    private final CardRepository cardRepository;
    private final TopicRepository topicRepository;
    private final CardMapper cardMapper;
    private final UserSubscriptionRepository userSubscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> getUnlearnedCardsByTopic(Long userId, Long topicId) {
        log.info("Fetching cards for topicId={}, userId={}", topicId, userId);

        Topic topic = topicRepository.findByIdWithDeck(topicId)
                .orElseThrow(() -> {
                    log.warn("Topic not found: topicId={}", topicId);
                    return new AppException(AppError.TOPIC_NOT_FOUND);
                });

        if (topic.getDeck().getIsPremium()) {
            boolean hasActiveSubscription = userSubscriptionRepository
                    .existsByUserIdAndStatusAndEndAtAfter(userId, SubscriptionStatus.ACTIVE, LocalDateTime.now());
            if (!hasActiveSubscription) {
                log.warn("User {} tried to access topic {} of premium deck without active subscription",
                        userId, topicId);
                throw new AppException(AppError.DECK_PREMIUM_REQUIRED);
            }
        }

        List<CardResponse> cards = cardRepository
                .findAllUnlearnedCardsByTopicIdWithPhonetics(topicId, userId)
                .stream()
                .map(cardMapper::toCardResponse)
                .toList();

        log.info("Fetched {} card(s) for topicId={}", cards.size(), topicId);
        return cards;
    }
}