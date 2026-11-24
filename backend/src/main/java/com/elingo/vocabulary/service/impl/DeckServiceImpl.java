package com.elingo.vocabulary.service.impl;

import com.elingo.vocabulary.dto.request.*;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.enums.SystemFileModule;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.util.SlugUtils;
import com.elingo.progress.service.ProgressService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.config.FlashcardProperties;
import com.elingo.vocabulary.dto.request.DeckSearchRequest;
import com.elingo.vocabulary.dto.request.UserDeckRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.*;
import com.elingo.vocabulary.mapper.DeckMapper;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.repository.CefrLevelRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TagRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.repository.specification.DeckFilter;
import com.elingo.vocabulary.repository.specification.DeckSpecifications;
import com.elingo.vocabulary.service.DeckAccessPolicy;
import com.elingo.vocabulary.service.DeckService;
import com.elingo.file.util.MediaKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DECK-SERVICE")
public class DeckServiceImpl implements DeckService {

    private final DeckRepository deckRepository;
    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;
    private final DeckMapper deckMapper;
    private final ProgressService progressService;
    private final DeckAccessPolicy accessPolicy;
    private final FlashcardProperties flashcardProperties;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final CefrLevelRepository cefrLevelRepository;
    private final MediaKeyResolver mediaKeyResolver;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeckResponse> getPublishedDecks(DeckSearchRequest req,
                                                        Pageable pageable,
                                                        Long userId) {
        DeckFilter f = DeckFilter.fromRequest(req)
                .statuses(Set.of(DeckStatus.PUBLISHED))
                .build();
        Page<Deck> page = deckRepository.findAll(DeckSpecifications.from(f), pageable);

        Map<Long, Integer> learned = userId == null
                ? Map.of()
                : progressService.getLearnedCardCountByDeck(
                        userId, page.getContent().stream().map(Deck::getId).toList());

        return PageResponse.of(page.map(deck -> deckMapper.toDeckResponse(deck,
                userId == null ? null : learned.getOrDefault(deck.getId(), 0)))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DeckResponse getDeckById(Long deckId, Long userId) {
        DeckRepository.DeckAccessView deckAccess = deckRepository.findAccessViewById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));
        accessPolicy.assertCanView(deckAccess, userId);

        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));
        return deckMapper.toDeckResponse(deck);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByDeckId(Long deckId, Long userId) {
        DeckRepository.DeckAccessView deck = deckRepository.findAccessViewById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));
        accessPolicy.assertCanLearn(deck, userId);

        return getTopicResponseInDeck(userId, deckId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByDeckSlug(String slug, Long userId) {
        DeckRepository.DeckAccessView deck = deckRepository.findAccessViewBySlug(slug)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));
        accessPolicy.assertCanLearn(deck, userId);

        return getTopicResponseInDeck(userId, deck.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeckResponse> getUserDecks(Long userId) {
        return deckRepository.findAllByOwnerTypeAndOwnerId(OwnerType.USER, userId).stream()
                .map(deckMapper::toDeckResponse).toList();
    }

    @Override
    @Transactional
    public DeckResponse createUserDeck(Long userId, UserDeckRequest request) {
        User owner = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        if (deckRepository.countByOwnerTypeAndOwnerId(OwnerType.USER, userId) >= flashcardProperties
                .getUserDeckLimit().getMaxDecksPerUser())
            throw new AppException(AppError.DECK_LIMIT_EXCEEDED);

        Deck deck = deckRepository.save(Deck.builder()
                .title(request.title())
                .slug(SlugUtils.generateUniqueSlug(request.title()))
                .description(request.description())
                .isPremium(false)
                .ownerType(OwnerType.USER)
                .owner(owner)
                .build());

        log.info("User deck created deckId={} slug={}", deck.getId(), deck.getSlug());
        return deckMapper.toDeckResponse(deck);
    }

    @Override
    @Transactional
    public DeckResponse updateUserDeck(Long userId, Long deckId, UserDeckRequest request) {
        assertCanEdit(userId, deckId);
        Deck deck = deckRepository.findById(deckId)
                        .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        deckMapper.updateDeck(deck, request);

        log.info("User deck updated deckId={}", deck.getId());
        return deckMapper.toDeckResponse(deck);
    }

    @Override
    @Transactional
    public DeckResponse setUserDeckPublished(Long userId, Long deckId, boolean published) {
        assertCanEdit(userId, deckId);
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        if (published) {
            if (deck.getStatus() != DeckStatus.PUBLISHED) {
                if (deck.getCardCount() == 0) {
                    throw new AppException(AppError.DECK_EMPTY);
                }
                deck.setStatus(DeckStatus.PUBLISHED);
                deck.setPublishedAt(LocalDateTime.now());
            }
        } else {
            deck.setStatus(DeckStatus.DRAFT);
            deck.setPublishedAt(null);
        }

        log.info("User deck publication changed deckId={} published={}", deckId, published);
        return deckMapper.toDeckResponse(deck);
    }

    @Override
    @Transactional
    public void deleteUserDeck(Long userId, Long deckId) {
        assertCanEdit(userId, deckId);
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        deckRepository.delete(deck);
        log.info("User deck deleted deckId={}", deckId);
    }

    // ==========================================
    // Admin Operations
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeckResponse> searchDecksForAdmin(AdminDeckSearchRequest request, Pageable pageable) {
        Specification<Deck> spec = DeckSpecifications.from(DeckFilter.fromAdminRequest(request));
        Page<Deck> page = deckRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(deckMapper::toDeckResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public DeckResponse getDeckByIdForAdmin(Long deckId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));
        return deckMapper.toDeckResponse(deck);
    }

    @Override
    @Transactional
    public DeckResponse createSystemDeck(SystemDeckRequest request) {
        var cover = mediaKeyResolver.forCreate(request.coverImageUrl(), SystemFileModule.DECKS);

        Deck deck = deckMapper.toDeck(request);
        deck.setSlug(SlugUtils.generateUniqueSlug(request.title()));
        deck.setStatus(DeckStatus.DRAFT);
        deck.setOwnerType(OwnerType.SYSTEM);
        deck.setOwner(null);
        deck.setCoverImageUrl(cover.key());
        deck.setTopicCount(0);
        deck.setCardCount(0);
        deck.setTags(resolveTags(request.tagIds()));
        deck.setCefrLevels(resolveCefrLevels(request.cefrLevelIds()));

        Deck saved = deckRepository.save(deck);
        mediaKeyResolver.publish(cover);

        log.info("System deck created deckId={} slug={}", saved.getId(), saved.getSlug());
        return deckMapper.toDeckResponse(saved);
    }

    @Override
    @Transactional
    public DeckResponse updateSystemDeck(Long deckId, SystemDeckRequest request) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        if (deck.getOwnerType() != OwnerType.SYSTEM) {
            throw new AppException(AppError.DECK_NOT_SYSTEM);
        }

        var cover = mediaKeyResolver.forUpdate(request.coverImageUrl(),
                deck.getCoverImageUrl(), SystemFileModule.DECKS);

        deckMapper.updateSystemDeck(deck, request);
        deck.setCoverImageUrl(cover.key());
        deck.setTags(resolveTags(request.tagIds()));
        deck.setCefrLevels(resolveCefrLevels(request.cefrLevelIds()));

        mediaKeyResolver.publish(cover);

        log.info("System deck updated deckId={}", deckId);
        return deckMapper.toDeckResponse(deck);
    }

    @Override
    @Transactional
    public DeckResponse updateDeckStatus(Long deckId, UpdateDeckStatusRequest request) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        if (deck.getOwnerType() != OwnerType.SYSTEM) {
            throw new AppException(AppError.DECK_NOT_SYSTEM);
        }

        DeckStatus targetStatus = request.status();
        switch (targetStatus) {
            case DeckStatus.PUBLISHED -> {
                if (deck.getCardCount() == 0) {
                    throw new AppException(AppError.DECK_EMPTY);
                }
                if (deck.getStatus() != DeckStatus.PUBLISHED) {
                    deck.setStatus(DeckStatus.PUBLISHED);
                    deck.setPublishedAt(LocalDateTime.now());
                }
            }
            case DeckStatus.ARCHIVED -> deck.setStatus(DeckStatus.ARCHIVED);
            default -> {
                deck.setStatus(DeckStatus.DRAFT);
                deck.setPublishedAt(null);
            }
        }

        log.info("System deck status changed deckId={} status={}", deckId, deck.getStatus());
        return deckMapper.toDeckResponse(deck);
    }

    @Override
    @Transactional
    public void deleteUserDeckByAdmin(Long deckId, String reason) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        if (deck.getOwnerType() == OwnerType.SYSTEM) {
            throw new AppException(AppError.CANNOT_DELETE_SYSTEM_DECK);
        }

        deckRepository.delete(deck);
        log.info("User deck deleted by admin deckId={} reason={}", deckId, reason);
    }

    // ==========================================
    // Helper Methods
    // ==========================================
    private Set<Tag> resolveTags(Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size())
            throw new AppException(AppError.TAG_NOT_FOUND);

        return new HashSet<>(tags);
    }

    private Set<CefrLevel> resolveCefrLevels(Set<Long> cefrLevelIds) {
        if (cefrLevelIds == null || cefrLevelIds.isEmpty()) {
            return new HashSet<>();
        }

        List<CefrLevel> cefrLevels = cefrLevelRepository.findAllById(cefrLevelIds);
        if (cefrLevels.size() != cefrLevelIds.size())
            throw new AppException(AppError.CEFR_LEVEL_NOT_FOUND);

        return new HashSet<>(cefrLevels);
    }

    private List<TopicResponse> getTopicResponseInDeck(Long userId, Long deckId) {
        List<Topic> topics = topicRepository.findByDeckId(deckId,
                Sort.by("order").and(Sort.by("id")));

        if (topics.isEmpty()) return List.of();

        Map<Long, Integer> learned = userId == null
                ? Map.of()
                : progressService.getLearnedCardCountByTopic(
                userId, topics.stream().map(Topic::getId).toList());

        return topics.stream()
                .map(t -> topicMapper.toTopicResponse(
                        t, userId == null ? null : learned.getOrDefault(t.getId(), 0))
                ).toList();
    }

    private void assertCanEdit(Long userId, Long deckId) {
        DeckRepository.DeckAccessView view = deckRepository.findAccessViewById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));
        accessPolicy.assertCanEdit(view, userId);
    }
}