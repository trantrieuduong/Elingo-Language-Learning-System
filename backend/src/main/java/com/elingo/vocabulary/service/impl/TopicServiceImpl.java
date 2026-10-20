package com.elingo.vocabulary.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
    public List<CardResponse> getCardsByTopic(Long userId, Long topicId) {
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

        if (topicRepository.countByDeckId(deckId) >= flashcardProperties
                .getUserDeckLimit().getMaxTopicsPerDeck())
            throw new AppException(AppError.TOPIC_LIMIT_EXCEEDED);

        Topic topic = topicRepository.save(Topic.builder()
                .deck(deckRepository.getReferenceById(deckId))
                .name(request.name())
                .slug(SlugUtils.generateSlug(request.name()))
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
        Long deckId = assertCanEditByTopic(userId, topicId).getId();

        int detached = cardRepository.detachByTopicId(topicId);
        topicRepository.deleteById(topicId);

        deckRepository.adjustTopicCount(deckId, -1);
        if (detached > 0)
            deckRepository.adjustCardCount(deckId, -detached);

        log.info("User topic deleted topicId={} detachedCards={}", topicId, detached);
    }

    private DeckRepository.DeckAccessView assertCanEditByTopic(Long userId, Long topicId) {
        DeckRepository.DeckAccessView view = deckRepository.findAccessViewByTopicId(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));
        accessPolicy.assertCanEdit(view, userId);
        return view;
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