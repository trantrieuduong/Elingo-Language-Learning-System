package com.elingo.vocabulary.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.vocabulary.dto.request.GetPublishedDecksRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.mapper.DeckMapper;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.DeckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DECK-SERVICE")
public class DeckServiceImpl implements DeckService {
    private static final int PAGE_SIZE = 9;

    private final DeckRepository deckRepository;
    private final TopicRepository topicRepository;
    private final CardRepository cardRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final TopicMapper topicMapper;
    private final DeckMapper deckMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeckResponse> getAllPublishedDecks(Long userId, GetPublishedDecksRequest request) {
        log.info("Fetching all published decks for userId={}, cefrCode={}, tagCode={}, keyword='{}', page={}",
                userId, request.cefrCode(), request.tagCode(), request.keyword(), request.page());

        String cefrCode = StringUtils.hasText(request.cefrCode()) ? request.cefrCode().trim() : null;
        String tagCode = StringUtils.hasText(request.tagCode()) ? request.tagCode().trim() : null;
        String keyword = StringUtils.hasText(request.keyword()) ? request.keyword().trim() : null;

        Pageable pageable = PageRequest.of(request.page() - 1, PAGE_SIZE);

        Page<DeckResponse> resultPage = deckRepository
                .findPublishedWithFilters(cefrCode, tagCode, keyword, pageable)
                .map(deckMapper::toDeckResponse);

        log.info("Fetched {}/{} published deck(s) (page {}/{}) for userId={}",
                resultPage.getNumberOfElements(), resultPage.getTotalElements(),
                resultPage.getNumber() + 1, resultPage.getTotalPages(), userId);

        return PageResponse.of(resultPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByDeck(Long userId, Long deckId) {
        log.info("Fetching topics for deckId={}, userId={}", deckId, userId);

        validateDeckAccess(userId, deckId);

        List<CardRepository.TopicUnlearnedCardCount> unlearnedCounts =
                cardRepository.countUnlearnedCardsByDeckIdGroupByTopic(deckId, userId);
        Map<Long, Integer> unlearnedMap = unlearnedCounts.stream()
                .collect(Collectors.toMap(
                        CardRepository.TopicUnlearnedCardCount::getTopicId,
                        CardRepository.TopicUnlearnedCardCount::getUnlearnedCardCount
                ));

        List<TopicResponse> topics = topicRepository
                .findAllByDeckIdOrderByOrderAsc(deckId)
                .stream()
                .map(topic -> {
                    TopicResponse response = topicMapper.toTopicResponse(topic);
                    return new TopicResponse(
                            response.id(),
                            response.name(),
                            response.slug(),
                            response.order(),
                            response.cardCount(),
                            unlearnedMap.getOrDefault(topic.getId(), 0)
                    );
                })
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