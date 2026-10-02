package com.elingo.vocabulary.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.mapper.DeckMapper;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.DeckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DECK-SERVICE")
public class DeckServiceImpl implements DeckService {
    private final DeckRepository deckRepository;
    private final TopicRepository topicRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final TopicMapper topicMapper;
    private final DeckMapper deckMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DeckResponse> getAllPublishedDecks(Long userId) {
        log.info("Fetching all published decks for userId = {}", userId);

        List<DeckResponse> decks = deckRepository
                .findAllByStatusWithDetails(DeckStatus.PUBLISHED)
                .stream()
                .map(deckMapper::toDeckResponse)
                .toList();

        log.info("Fetched {} published deck(s) for userId={}", decks.size(), userId);
        return decks;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByDeck(Long userId, Long deckId) {
        log.info("Fetching topics for deckId={}, userId={}", deckId, userId);

        validateDeckAccess(userId, deckId);
        List<TopicResponse> topics = topicRepository
                .findAllByDeckIdOrderByOrderAsc(deckId)
                .stream()
                .map(topicMapper::toTopicResponse)
                .toList();

        log.info("Fetched {} topic(s) for deckId={}", topics.size(), deckId);
        return topics;
    }

    private void validateDeckAccess(Long userId, Long deckId) {
        var deck = deckRepository.findById(deckId)
                .orElseThrow(() -> {
                    log.warn("Deck not found: deckId={}", deckId);
                    return new AppException(AppError.DECK_NOT_FOUND);
                });

        if (deck.getStatus() != DeckStatus.PUBLISHED) {
            log.warn("Deck not published: deckId={}, status={}", deckId, deck.getStatus());
            throw new AppException(AppError.DECK_NOT_FOUND);
        }

        if (deck.getIsPremium()) {
            boolean hasActiveSubscription = userSubscriptionRepository
                    .existsByUserIdAndStatusAndEndAtAfter(userId, SubscriptionStatus.ACTIVE, LocalDateTime.now());
            if (!hasActiveSubscription) {
                log.warn("User {} tried to access premium deck {} without active subscription",
                        userId, deckId);
                throw new AppException(AppError.DECK_PREMIUM_REQUIRED);
            }
        }
        log.info("Deck access granted: deckId={}, userId={}", deckId, userId);
    }
}