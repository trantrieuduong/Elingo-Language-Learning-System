package com.elingo.vocabulary.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.dto.request.CreateDeckRequest;
import com.elingo.vocabulary.dto.request.GetPublishedDecksRequest;
import com.elingo.vocabulary.dto.request.UpdateDeckRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.*;
import com.elingo.vocabulary.mapper.DeckMapper;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.repository.*;
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
import java.util.HashSet;
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
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final CefrLevelRepository cefrLevelRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final TopicMapper topicMapper;
    private final DeckMapper deckMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeckResponse> getAllPublishedDecks(Long userId, GetPublishedDecksRequest request) {
        String cefrCode = StringUtils.hasText(request.cefrCode()) ? request.cefrCode().trim() : null;
        String tagCode = StringUtils.hasText(request.tagCode()) ? request.tagCode().trim() : null;
        String keyword = StringUtils.hasText(request.keyword()) ? request.keyword().trim() : null;

        Pageable pageable = PageRequest.of(request.page() - 1, PAGE_SIZE);

        Page<Deck> deckPage = deckRepository
                .findPublishedWithFilters(cefrCode, tagCode, keyword, pageable);

        List<Long> deckIds = deckPage.getContent().stream()
                .map(Deck::getId)
                .toList();

        Map<Long, Integer> learnedMap;
        if (userId != null && !deckIds.isEmpty()) {
            learnedMap = cardRepository.countLearnedCardsByDeckIds(deckIds, userId)
                    .stream()
                    .collect(Collectors.toMap(
                            CardRepository.DeckLearnedCardCount::getDeckId,
                            CardRepository.DeckLearnedCardCount::getLearnedCardCount
                    ));
        } else {
            learnedMap = Map.of();
        }

        Page<DeckResponse> resultPage = deckPage.map(deck ->
                deckMapper.toDeckResponse(deck, learnedMap.getOrDefault(deck.getId(), 0))
        );

        log.info("Published decks fetched count={} total={} page={} totalPages={}",
                resultPage.getNumberOfElements(), resultPage.getTotalElements(),
                resultPage.getNumber() + 1, resultPage.getTotalPages());

        return PageResponse.of(resultPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByDeck(Long userId, Long deckId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));
        validateDeckAccess(userId, deck);

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
                .map(topic -> topicMapper.toTopicResponse(topic, unlearnedMap.getOrDefault(topic.getId(), 0)))
                .toList();

        log.info("Topics fetched count={} deckId={}", topics.size(), deckId);
        return topics;
    }

    private void validateDeckAccess(Long userId, Deck deck) {
        if (deck.getStatus() != DeckStatus.PUBLISHED) {
            throw new AppException(AppError.DECK_NOT_FOUND);
        }

        if (deck.getIsPremium()) {
            boolean hasActiveSubscription = userSubscriptionRepository
                    .existsByUserIdAndStatusAndEndAtAfter(userId, SubscriptionStatus.ACTIVE, LocalDateTime.now());
            if (!hasActiveSubscription) {
                throw new AppException(AppError.DECK_PREMIUM_REQUIRED);
            }
        }
        log.info("Deck access granted deckId={}", deck.getId());
    }

    @Override
    @Transactional
    public DeckResponse createUserDeck(CreateDeckRequest request, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        Deck deck = createDeckCoreLogic(request, OwnerType.USER, user);
        return deckMapper.toDeckResponse(deck);
    }

    @Override
    @Transactional
    public DeckResponse createSystemDeck(CreateDeckRequest request) {
        Deck deck = createDeckCoreLogic(request, OwnerType.SYSTEM, null);
        return deckMapper.toDeckResponse(deck);
    }

    private Deck createDeckCoreLogic(CreateDeckRequest request, OwnerType ownerType, User owner) {
        if (deckRepository.existsBySlug(request.slug())) {
            throw new AppException(AppError.DECK_SLUG_EXISTED);
        }

        Deck deck = new Deck();
        deck.setTitle(request.title());
        deck.setSlug(request.slug());
        deck.setDescription(request.description());
        deck.setCoverImageUrl(request.coverImageUrl());
        deck.setStatus(DeckStatus.DRAFT);
        deck.setOwnerType(ownerType);
        deck.setOwner(owner);
        deck.setTopicCount(0);
        deck.setCardCount(0);

        if (ownerType == OwnerType.SYSTEM) {
            deck.setIsPremium(Boolean.TRUE.equals(request.isPremium()));
        } else {
            deck.setIsPremium(false);
        }

        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(request.tagIds());
            deck.setTags(new HashSet<>(tags));
        }

        if (request.cefrLevelIds() != null && !request.cefrLevelIds().isEmpty()) {
            List<CefrLevel> cefrLevels = cefrLevelRepository.findAllById(request.cefrLevelIds());
            deck.setCefrLevels(new HashSet<>(cefrLevels));
        }
        return deckRepository.save(deck);
    }

    @Override
    @Transactional
    public DeckResponse updateUserDeck(Long deckId, UpdateDeckRequest request, Long currentUserId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        if (deck.getOwnerType() != OwnerType.USER || !deck.getOwner().getId().equals(currentUserId)) {
            throw new AppException(AppError.DECK_ACCESS_DENIED);
        }
        return deckMapper.toDeckResponse(updateDeckCoreLogic(deck, request, false));
    }

    @Override
    @Transactional
    public DeckResponse updateSystemDeck(Long deckId, UpdateDeckRequest request) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        if (deck.getOwnerType() != OwnerType.SYSTEM) {
            throw new AppException(AppError.DECK_ACCESS_DENIED);
        }
        return deckMapper.toDeckResponse(updateDeckCoreLogic(deck, request, true));
    }

    private Deck updateDeckCoreLogic(Deck deck, UpdateDeckRequest request, boolean isAdmin) {
        if (deckRepository.existsBySlugAndIdNot(request.slug(), deck.getId())) {
            throw new AppException(AppError.DECK_SLUG_EXISTED);
        }

        deck.setTitle(request.title());
        deck.setSlug(request.slug());
        deck.setDescription(request.description());
        deck.setCoverImageUrl(request.coverImageUrl());

        if (request.status() != null) {
            deck.setStatus(request.status());
            if (request.status() == DeckStatus.PUBLISHED && deck.getPublishedAt() == null) {
                deck.setPublishedAt(LocalDateTime.now());
            }
        }

        if (request.tagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(request.tagIds());
            if (deck.getTags() == null) {
                deck.setTags(new HashSet<>());
            }

            // Xóa các phần tử cũ không có trong danh sách mới
            deck.getTags().retainAll(tags);
            // Chỉ thêm các phần tử mới chưa có trong danh sách cũ
            deck.getTags().addAll(tags);
        }

        if (request.cefrLevelIds() != null) {
            List<CefrLevel> cefrLevels = cefrLevelRepository.findAllById(request.cefrLevelIds());
            if (deck.getCefrLevels() == null) {
                deck.setCefrLevels(new HashSet<>());
            }
            deck.getCefrLevels().retainAll(cefrLevels);
            deck.getCefrLevels().addAll(cefrLevels);
        }

        if (isAdmin && request.isPremium() != null) {
            deck.setIsPremium(request.isPremium());
        }
        return deckRepository.save(deck);
    }

    @Override
    @Transactional
    public void deleteDeck(Long deckId, Long currentUserId, boolean isAdmin) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        if (!isAdmin) {
            if (deck.getOwnerType() != OwnerType.USER || !deck.getOwner().getId().equals(currentUserId)) {
                throw new AppException(AppError.DECK_ACCESS_DENIED);
            }
        } else {
            if (deck.getOwnerType() != OwnerType.SYSTEM) {
                throw new AppException(AppError.DECK_ACCESS_DENIED);
            }
        }

        if (deck.getPublishedAt() != null) {
            deck.setStatus(DeckStatus.ARCHIVED);
            deck.setTopicCount(0);
            deck.setCardCount(0);
            deckRepository.save(deck);
            topicRepository.softDeleteByDeckId(deck.getId());
            cardRepository.softDeleteByDeckId(deck.getId());

            log.info("Deck Soft-deleted successfully, deckId={}", deck.getId());
        } else {
            // Database tự động xóa Card, Topic nhờ ON DELETE CASCADE
            deckRepository.delete(deck);

            log.info("Deck Hard-deleted successfully, deckId={}", deck.getId());
        }
    }
}