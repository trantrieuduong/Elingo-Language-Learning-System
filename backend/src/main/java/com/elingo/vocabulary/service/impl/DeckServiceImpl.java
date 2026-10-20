package com.elingo.vocabulary.service.impl;

import com.elingo.common.dto.PageResponse;
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
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.mapper.DeckMapper;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.repository.specification.DeckFilter;
import com.elingo.vocabulary.repository.specification.DeckSpecifications;
import com.elingo.vocabulary.service.DeckAccessPolicy;
import com.elingo.vocabulary.service.DeckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public List<TopicResponse> getTopicsByDeck(Long deckId, Long userId) {
        DeckRepository.DeckAccessView deck = deckRepository.findAccessViewById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        accessPolicy.assertCanLearn(deck, userId);

        List<Topic> topics = topicRepository.findByDeckId(deck.getId(),
                Sort.by("order").and(Sort.by("id")));

        if (topics.isEmpty()) return List.of();

        Map<Long, Integer> learned = userId == null
                ? Map.of()
                : progressService.getLearnedCardCountByTopic(
                        userId, topics.stream().map(Topic::getId).toList());

        return topics.stream()
                .map(t -> topicMapper
                        .toTopicResponse(t, userId == null ? null : learned.getOrDefault(t.getId(), 0)))
                .toList();
    }

    @Override
    @Transactional
    public DeckResponse createUserDeck(Long userId, UserDeckRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        if (deckRepository.countByOwnerTypeAndOwnerId(OwnerType.USER, userId) >= flashcardProperties
                .getUserDeckLimit().getMaxDecksPerUser())
            throw new AppException(AppError.DECK_LIMIT_EXCEEDED);

        Deck deck = deckRepository.save(Deck.builder()
                .title(request.title())
                .slug(SlugUtils.generateSlug(request.title()))
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

        // topics are removed and cards detached by the database (@OnDelete), no cascade needed here
        deckRepository.deleteById(deckId);

        log.info("User Deck deleted deckId={}", deckId);
    }

    private void assertCanEdit(Long userId, Long deckId) {
        DeckRepository.DeckAccessView view = deckRepository.findAccessViewById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));
        accessPolicy.assertCanEdit(view, userId);
    }
}