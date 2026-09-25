package com.elingo.vocabulary.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.flashcard.dto.response.QuizOptionResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TOPIC-SERVICE")
public class TopicServiceImpl implements TopicService {
    private final CardRepository cardRepository;
    private final TopicRepository topicRepository;
    private final CardMapper cardMapper;

    @Value("${app.flashcard.distractor-options}")
    private int distractorOptions;

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> getCardsByTopic(Long userId, Long topicId) {
        log.info("Fetching cards for topicId={}, userId={}", topicId, userId);

        if (!topicRepository.existsById(topicId)) {
            log.warn("Topic not found: topicId={}", topicId);
            throw new AppException(AppError.TOPIC_NOT_FOUND);
        }

        List<CardResponse> cards = cardRepository
                .findAllUnlearnedCardsByTopicIdWithPhonetics(topicId, userId)
                .stream()
                .map(cardMapper::toCardResponse)
                .toList();

        log.info("Fetched {} card(s) for topicId={}", cards.size(), topicId);
        return cards;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizOptionResponse> getQuizOptions(Long userId, Long topicId, Long cardId) {
        log.info("Processing get quiz options for userId={}, topicId={}, cardId={}", userId, topicId, cardId);

        if (!topicRepository.existsById(topicId)) {
            log.warn("Topic not found: topicId={}, userId={}", topicId, userId);
            throw new AppException(AppError.TOPIC_NOT_FOUND);
        }

        Card targetCard = cardRepository.findByIdAndTopicIdWithPhonetics(cardId, topicId)
                .orElseThrow(() -> {
                    log.warn("Card not found or does not belong to topic: cardId={}, topicId={}, userId={}",
                            cardId, topicId, userId);
                    return new AppException(AppError.CARD_NOT_FOUND);
                });

        String correctTerm = targetCard.getTerm();
        List<Card> distractorCards = fetchDistractors(topicId, targetCard);
        if (distractorCards.isEmpty()) {
            log.warn("Not enough distractor cards in deck: cardId={}, topicId={}, userId={}", cardId, topicId, userId);
            throw new AppException(AppError.QUIZ_NOT_ENOUGH_CARDS);
        }

        List<QuizOptionResponse> options = buildOptions(correctTerm, distractorCards);
        log.info("Quiz generated: cardId={}, topicId={}, userId={}, optionCount={}",
                cardId, topicId, userId, options.size());
        return options;
    }

    /**
     * Lấy đáp án sai ở cùng topic trước, fallback ở deck
     */
    private List<Card> fetchDistractors(Long topicId, Card targetCard) {
        int OVER_FETCH_LIMIT = distractorOptions + 10;
        // Lấy dư để tránh thiếu card khi filter

        List<Card> topicPool = cardRepository.findRandomDistractorsByTopicId(topicId, targetCard.getId(),
                PageRequest.of(0, OVER_FETCH_LIMIT));

        List<Card> finalPool = new ArrayList<>(topicPool);
        // Do list topicPool query từ DB là dạng không thể chỉnh sửa

        if (finalPool.size() < OVER_FETCH_LIMIT) {
            List<Card> deckPool = cardRepository.findRandomDistractorsByDeckId(
                    targetCard.getDeck().getId(), targetCard.getId(), PageRequest.of(0, OVER_FETCH_LIMIT));
            finalPool.addAll(deckPool);
        }
        return finalPool;
    }

    private List<QuizOptionResponse> buildOptions(String correctTerm, List<Card> distractorCards) {
        List<QuizOptionResponse> options = new ArrayList<>();
        options.add(new QuizOptionResponse(correctTerm, true));

        Set<String> seenTerms = new HashSet<>();
        seenTerms.add(correctTerm.trim().toLowerCase());

        distractorCards.stream()
                .map(Card::getTerm)
                .filter(StringUtils::hasText)
                //.hasText false khi string null, "", " ", "\n", "\t
                .map(String::trim)
                .filter(t -> seenTerms.add(t.toLowerCase()))
                .limit(distractorOptions)
                .forEach(t -> options.add(new QuizOptionResponse(t, false)));

        Collections.shuffle(options);
        return options;
    }
}