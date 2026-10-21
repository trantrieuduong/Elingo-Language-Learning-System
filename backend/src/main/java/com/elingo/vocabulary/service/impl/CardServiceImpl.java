package com.elingo.vocabulary.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.vocabulary.config.FlashcardProperties;
import com.elingo.vocabulary.dto.request.CreateUserCardRequest;
import com.elingo.vocabulary.dto.request.UpdateUserCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.CardService;
import com.elingo.vocabulary.service.DeckAccessPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CARD-SERVICE")
public class CardServiceImpl implements CardService {

    private final DeckRepository deckRepository;
    private final TopicRepository topicRepository;
    private final CardRepository cardRepository;
    private final FlashcardProperties flashcardProperties;
    private final DeckAccessPolicy accessPolicy;
    private final CardMapper cardMapper;

    @Override
    @Transactional
    public CardResponse createUserCard(Long userId, CreateUserCardRequest request) {
        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));
        DeckRepository.DeckAccessView deckView = assertCanEditByTopic(userId, request.topicId());
        Long deckId = deckView.getId();

        long currentCardCount = cardRepository.countByTopicId(request.topicId());
        int maxCards = flashcardProperties.getUserDeckLimit().getMaxCardsPerTopic();
        if (currentCardCount >= maxCards)
            throw new AppException(AppError.CARD_LIMIT_EXCEEDED);


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
        DeckRepository.DeckAccessView deckView = assertCanEditByCard(userId, cardId);
        Long deckId = deckView.getId();

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));

        Long topicId = card.getTopic().getId();

        cardRepository.delete(card);
        topicRepository.adjustCardCount(topicId, -1);
        deckRepository.adjustCardCount(deckId, -1);

        log.info("User card deleted cardId={} topicId={} deckId={}", cardId, topicId, deckId);
    }

    private DeckRepository.DeckAccessView assertCanEditByTopic(Long userId, Long topicId) {
        DeckRepository.DeckAccessView view = deckRepository.findAccessViewByTopicId(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));
        accessPolicy.assertCanEdit(view, userId);
        return view;
    }
    private DeckRepository.DeckAccessView assertCanEditByCard(Long userId, Long cardId) {
        DeckRepository.DeckAccessView view = deckRepository.findAccessViewByCardId(cardId)
                .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));
        accessPolicy.assertCanEdit(view, userId);
        return view;
    }
    private int calculateNextOrder(Long topicId) {
        return cardRepository.findFirstByTopicIdOrderByOrderDesc(topicId)
                .map(c -> c.getOrder() + 1)
                .orElse(1);
    }
}
