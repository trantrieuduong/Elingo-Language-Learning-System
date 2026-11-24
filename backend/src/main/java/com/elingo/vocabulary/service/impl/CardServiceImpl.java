package com.elingo.vocabulary.service.impl;

import com.elingo.vocabulary.dto.request.CardPhoneticRequest;
import com.elingo.vocabulary.dto.request.CreateSystemCardRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemCardRequest;
import com.elingo.vocabulary.dto.response.OrphanedCardCleanupResponse;
import com.elingo.vocabulary.dto.response.OrphanedCardStatsResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.enums.SystemFileModule;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.file.util.FileKey;
import com.elingo.vocabulary.config.FlashcardProperties;
import com.elingo.vocabulary.dto.request.CreateUserCardRequest;
import com.elingo.vocabulary.dto.request.UpdateUserCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.*;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.mapper.CardPhoneticMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.CardService;
import com.elingo.vocabulary.service.DeckAccessPolicy;
import com.elingo.file.util.MediaKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CARD-SERVICE")
public class CardServiceImpl implements CardService {

    private static final int BATCH_SIZE = 500;

    private final DeckRepository deckRepository;
    private final TopicRepository topicRepository;
    private final CardRepository cardRepository;
    private final FlashcardProperties flashcardProperties;
    private final DeckAccessPolicy accessPolicy;
    private final CardMapper cardMapper;
    private final CardPhoneticMapper cardPhoneticMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final MediaKeyResolver mediaKeyResolver;

    @Override
    @Transactional
    public CardResponse createUserCard(Long userId, CreateUserCardRequest request) {
        Long topicId = request.topicId();
        Long deckId = assertCanEditByTopic(userId, topicId).getId();

        Topic topic = topicRepository.findByIdForUpdate(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));

        if (cardRepository.countByTopicId(topicId)
                >= flashcardProperties.getUserDeckLimit().getMaxCardsPerTopic()) {
            throw new AppException(AppError.CARD_LIMIT_EXCEEDED);
        }

        int nextOrder = calculateNextOrder(request.topicId());
        Card card = cardMapper.toCard(request);
        card.setDeck(deckRepository.getReferenceById(deckId));
        card.setTopic(topic);
        card.setOrder(nextOrder);
        card.setPhonetics(new ArrayList<>());

        Card savedCard = cardRepository.save(card);
        topicRepository.adjustCardCount(request.topicId(), 1);
        deckRepository.adjustCardCount(deckId, 1);

        log.info("User card created cardId={} topicId={} deckId={}",
                savedCard.getId(), request.topicId(), deckId);
        return cardMapper.toCardResponse(savedCard);
    }

    @Override
    @Transactional
    public CardResponse updateUserCard(Long userId, Long cardId, UpdateUserCardRequest request) {
        assertCanEditByCard(userId, cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));

        cardMapper.updateCard(card, request);

        log.info("User card updated cardId={}", cardId);
        return cardMapper.toCardResponse(card);
    }

    @Override
    @Transactional
    public void deleteUserCard(Long userId, Long cardId) {
        assertCanEditByCard(userId, cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));

        doDeleteCard(card);
        log.info("User card deleted cardId={}", cardId);
    }

    // ==========================================
    // Admin Operations
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> getCardsByTopicIdForAdmin(Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new AppException(AppError.TOPIC_NOT_FOUND);
        }
        List<Card> cards = cardRepository.findByTopicIdWithPhonetics(topicId);
        return cards.stream().map(cardMapper::toCardResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getCardByIdForAdmin(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));
        return cardMapper.toCardResponse(card);
    }

    @Override
    @Transactional
    public CardResponse createSystemCard(CreateSystemCardRequest request) {
        Long topicId = request.topicId();
        Topic topic = topicRepository.findByIdForUpdate(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));
        Deck deck = topic.getDeck();
        assertSystemDeck(deck);

        var image = mediaKeyResolver.forCreate(request.imageUrl(), SystemFileModule.CARDS);
        List<MediaKeyResolver.Resolved> audios = new ArrayList<>();

        Card card = cardMapper.toCard(request);
        card.setDeck(deck);
        card.setTopic(topic);
        card.setOrder(calculateNextOrder(topicId));
        card.setImageUrl(image.key());
        card.setPhonetics(new ArrayList<>());
        addPhonetics(card, request.phonetics(), audios);

        Card saved = cardRepository.save(card);
        topicRepository.adjustCardCount(topicId, 1);
        deckRepository.adjustCardCount(deck.getId(), 1);

        mediaKeyResolver.publish(image);
        audios.forEach(mediaKeyResolver::publish);

        log.info("System card created cardId={} topicId={} deckId={}", saved.getId(), topicId, deck.getId());
        return cardMapper.toCardResponse(saved);
    }

    @Override
    @Transactional
    public CardResponse updateSystemCard(Long cardId, UpdateSystemCardRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));
        assertSystemCard(card);

        var image = mediaKeyResolver.forUpdate(request.imageUrl(), card.getImageUrl(), SystemFileModule.CARDS);
        cardMapper.updateCardAdmin(card, request);
        card.setImageUrl(image.key());

        List<MediaKeyResolver.Resolved> audios = new ArrayList<>();
        List<String> obsoleteAudios = replacePhonetics(card, request.phonetics(), audios);

        mediaKeyResolver.publish(image);
        audios.forEach(mediaKeyResolver::publish);
        if (!obsoleteAudios.isEmpty()) {
            eventPublisher.publishEvent(new FileDeletedEvent(obsoleteAudios));
        }

        log.info("System card updated cardId={}", cardId);
        return cardMapper.toCardResponse(card);
    }

    @Override
    @Transactional
    public void reorderSystemCards(Long topicId, List<Long> orderedIds) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));
        assertSystemDeck(topic.getDeck());

        if (new HashSet<>(orderedIds).size() != orderedIds.size()
                || orderedIds.size() != cardRepository.countByTopicId(topicId)) {
            throw new AppException(AppError.INVALID_REQUEST);
        }
        Map<Long, Card> byId = cardRepository.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(Card::getId, Function.identity()));
        for (int i = 0; i < orderedIds.size(); i++) {
            Card card = byId.get(orderedIds.get(i));
            if (card == null) throw new AppException(AppError.CARD_NOT_FOUND);
            if (card.getTopic() == null || !card.getTopic().getId().equals(topicId)) {
                throw new AppException(AppError.INVALID_REQUEST);
            }
            card.setOrder(i + 1);
        }
    }

    @Override
    @Transactional
    public void deleteSystemCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));
        assertSystemCard(card);
        doDeleteCard(card);
        log.info("Card deleted by admin cardId={}", cardId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrphanedCardStatsResponse getOrphanedCardStats() {
        long total = cardRepository.countOrphanedCards();
        long safe = cardRepository.countSafeToDeleteOrphans();
        return new OrphanedCardStatsResponse(total, safe, total - safe);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getOrphanedCards(Pageable pageable) {
        Page<Card> page = cardRepository.findOrphanedCards(pageable);
        return PageResponse.of(page.map(cardMapper::toCardResponse));
    }

    @Override
    @Transactional
    public OrphanedCardCleanupResponse cleanupOrphanedCards() {
        List<Long> safeIds = cardRepository.findSafeToDeleteOrphanIds(BATCH_SIZE);
        if (safeIds.isEmpty()) {
            return new OrphanedCardCleanupResponse(0, 0, 0);
        }

        List<Card> safeCards = cardRepository.findAllByIdWithPhonetics(safeIds);
        List<String> files = collectMediaFiles(safeCards);

        cardRepository.deleteAll(safeCards);
        if (!files.isEmpty()) {
            eventPublisher.publishEvent(new FileDeletedEvent(files));
        }

        log.info("Orphaned cards cleaned up scanned={} deletedCards={} deletedFiles={}",
                safeIds.size(), safeCards.size(), files.size());
        return new OrphanedCardCleanupResponse(safeIds.size(), safeCards.size(), files.size());
    }

    // ==========================================
    // Helper Methods
    // ==========================================
    private void assertSystemDeck(Deck deck) {
        if (deck.getOwnerType() != OwnerType.SYSTEM)
            throw new AppException(AppError.DECK_NOT_SYSTEM);
    }

    private void assertSystemCard(Card card) {
        if (card.getDeck() == null)
            throw new AppException(AppError.CARD_NOT_AVAILABLE); // card mồ côi
        assertSystemDeck(card.getDeck());
    }

    private void addPhonetics(Card card,
                              List<CardPhoneticRequest> reqs,
                              List<MediaKeyResolver.Resolved> attachments) {
        if (reqs == null) return;
        Set<String> used = new HashSet<>();
        for (CardPhoneticRequest req : reqs) {
            String audioKey = resolveAudioKey(req.audioUrl(), Set.of(), attachments);
            if (audioKey != null && !used.add(audioKey)) {
                throw new AppException(AppError.INVALID_REQUEST);
            }
            card.getPhonetics().add(buildPhonetic(card, req, audioKey));
        }
    }

    /**
     * Trả về danh sách audio cũ cần xóa.
     */
    private List<String> replacePhonetics(Card card, List<CardPhoneticRequest> reqs,
                                          List<MediaKeyResolver.Resolved> attachments) {
        Set<String> oldAudios = card.getPhonetics().stream()
                .map(CardPhonetic::getAudioUrl).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> used = new HashSet<>();
        List<CardPhonetic> next = new ArrayList<>();

        for (CardPhoneticRequest req : reqs == null ? List.<CardPhoneticRequest>of() : reqs) {
            String audioKey = resolveAudioKey(req.audioUrl(), oldAudios, attachments);
            if (audioKey != null && !used.add(audioKey)) {
                throw new AppException(AppError.INVALID_REQUEST); // cùng một file gắn 2 lần
            }
            next.add(buildPhonetic(card, req, audioKey));
        }

        card.getPhonetics().clear();
        card.getPhonetics().addAll(next);

        return oldAudios.stream()
                .filter(a -> !used.contains(a) && FileKey.isPermanentKey(a))
                .toList();
    }

    private void doDeleteCard(Card card) {
        if (card.getTopic() != null) {
            topicRepository.adjustCardCount(card.getTopic().getId(), -1);
        }
        if (card.getDeck() != null) {
            deckRepository.adjustCardCount(card.getDeck().getId(), -1);
        }

        List<String> filesToDelete = collectMediaFiles(List.of(card));
        cardRepository.delete(card);

        if (!filesToDelete.isEmpty()) {
            eventPublisher.publishEvent(new FileDeletedEvent(filesToDelete));
        }
    }

    private List<String> collectMediaFiles(Collection<Card> cards) {
        List<String> files = new ArrayList<>();
        for (Card card : cards) {
            if (card.getImageUrl() != null && !card.getImageUrl().isBlank()) {
                files.add(card.getImageUrl());
            }
            if (card.getPhonetics() != null) {
                for (CardPhonetic cp : card.getPhonetics()) {
                    if (cp.getAudioUrl() != null && !cp.getAudioUrl().isBlank()) {
                        files.add(cp.getAudioUrl());
                    }
                }
            }
        }
        return files;
    }

    /**
     * null/blank → không có audio; key vĩnh viễn (uploads/ hoặc system/) → giữ file cũ; còn lại → file mới (verified/).
     */
    private String resolveAudioKey(String key, Set<String> oldAudios,
                                   List<MediaKeyResolver.Resolved> attachments) {
        if (key == null || key.isBlank()) return null;

        if (FileKey.isPermanentKey(key)) {
            if (!oldAudios.contains(key)) throw new AppException(AppError.INVALID_REQUEST);
            return key;
        }

        var resolved = mediaKeyResolver.forCreate(key, SystemFileModule.PHONETICS);
        attachments.add(resolved);
        return resolved.key();
    }

    private CardPhonetic buildPhonetic(Card card, CardPhoneticRequest req, String audioKey) {
        CardPhonetic phonetic = cardPhoneticMapper.toCardPhonetic(req);
        phonetic.setAudioUrl(audioKey);
        phonetic.setCard(card);
        return phonetic;
    }

    private DeckRepository.DeckAccessView assertCanEditByTopic(Long userId, Long topicId) {
        DeckRepository.DeckAccessView view = deckRepository.findAccessViewByTopicId(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));
        accessPolicy.assertCanEdit(view, userId);
        return view;
    }

    private void assertCanEditByCard(Long userId, Long cardId) {
        DeckRepository.DeckAccessView view = deckRepository.findAccessViewByCardId(cardId)
                .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));
        accessPolicy.assertCanEdit(view, userId);
    }

    private int calculateNextOrder(Long topicId) {
        return cardRepository.findFirstByTopicIdOrderByOrderDesc(topicId)
                .map(c -> c.getOrder() + 1)
                .orElse(1);
    }
}
