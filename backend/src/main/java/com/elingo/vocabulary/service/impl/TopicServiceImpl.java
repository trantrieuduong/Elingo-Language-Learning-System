package com.elingo.vocabulary.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.flashcard.dto.response.CardLearningState;
import com.elingo.learning.flashcard.service.CardStateService;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
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
    private final DeckAccessPolicy accessPolicy;
    private final CardRepository cardRepository;
    private final CardStateService cardStateService;
    private final CardMapper cardMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> getCardsByTopic(Long userId, Long topicId) {
        DeckRepository.DeckAccessView deck = deckRepository.findAccessViewByTopicId(topicId)
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));

        accessPolicy.assertCanLearn(deck, userId);

        List<Card> cards = cardRepository.findByTopicId(topicId);

        Map<Long, CardLearningState> states = (userId == null || cards.isEmpty())
                ? Map.of()
                : cardStateService.getStates(userId, cards.stream().map(Card::getId).toList());

        return cards.stream().map(card -> cardMapper.toCardResponse(card, userId == null
                ? null
                : states.getOrDefault(card.getId(), CardLearningState.NONE))
        ).toList();
    }
}