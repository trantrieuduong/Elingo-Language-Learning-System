package com.elingo.learning.flashcard.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.response.QuizOptionResponse;
import com.elingo.learning.flashcard.dto.response.QuizResponse;
import com.elingo.learning.flashcard.dto.response.StarCardResponse;
import com.elingo.learning.flashcard.dto.response.HideCardResponse;

import com.elingo.learning.flashcard.entity.UserCardState;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.learning.flashcard.service.FlashcardService;
import com.elingo.learning.flashcard.service.SpacedRepetitionService;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.dto.response.TopicResponse;
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
@Slf4j(topic = "FLASHCARD-SERVICE")
public class FlashcardServiceImpl implements FlashcardService {
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final TopicRepository topicRepository;
    private final UserCardStateRepository userCardStateRepository;
    private final SpacedRepetitionService spacedRepetitionService;
    private final CardMapper cardMapper;
    private final TopicMapper topicMapper;

    @Value("${app.flashcard.distractor-options}")
    private int distractorOptions;

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByDeck(Long userId, Long deckId) {
        log.info("Fetching topics for deckId={}, userId={}", deckId, userId);

        if (!deckRepository.existsById(deckId)) {
            log.warn("Deck not found: deckId={}", deckId);
            throw new AppException(AppError.DECK_NOT_FOUND);
        }

        List<TopicResponse> topics = topicRepository
                .findAllByDeckIdOrderByOrderAsc(deckId)
                .stream()
                .map(topicMapper::toTopicResponse)
                .toList();

        log.info("Fetched {} topic(s) for deckId={}", topics.size(), deckId);
        return topics;
    }

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
    public QuizResponse getQuizOptions(Long userId, Long topicId, Long cardId) {
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
        return new QuizResponse(
                options
        );
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
        return Collections.unmodifiableList(options);
    }

    @Override
    @Transactional
    public void submitSrsReview(Long userId, Long cardId, SrsReviewRequest request) {
        int grade = request.grade();
        log.info("Processing Srs review submission for userId={}, cardId={}, grade={}", userId, cardId, grade);

        UserCardState state = userCardStateRepository
                .findByCardIdAndUserId(cardId, userId)
                .orElseGet(() -> {
                    log.info("No existing UserCardState for userId={}, cardId={} - creating new record with srs", userId, cardId);
                    Card card = cardRepository.findById(cardId)
                            .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
                    return UserCardState.builder()
                            .card(card)
                            .deck(card.getDeck())
                            .topic(card.getTopic())
                            .user(user)
                            .build();
                });
        spacedRepetitionService.calculateNextSRS(state, grade);
        userCardStateRepository.save(state);

        log.info("Submit Srs review successfully for userId={}, cardId={}, grade={}, newInterval={}, nextReview={}",
                userId, cardId, grade, state.getSrsInterval(), state.getSrsNextReviewAt());

        // publishEvent FlashcardReviewedEvent
    }

    @Override
    @Transactional
    public StarCardResponse toggleStar(Long userId, Long cardId) {
        log.info("Processing toggle star for userId={}, cardId={}", userId, cardId);

        UserCardState state = userCardStateRepository
                .findByCardIdAndUserId(cardId, userId)
                .orElseGet(() -> {
                    log.info("No existing UserCardState for userId={}, cardId={} - creating new record with star / unstar state", userId, cardId);
                    Card card = cardRepository.findById(cardId)
                            .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
                    return UserCardState.builder()
                            .card(card)
                            .deck(card.getDeck())
                            .topic(card.getTopic())
                            .user(user)
                            .build();
                });

        boolean newStarredState = !state.getFlagsStarred();
        state.setFlagsStarred(newStarredState);
        userCardStateRepository.save(state);

        log.info("Toggle star successfully for userId={}, cardId={}, isStarred={}", userId, cardId, newStarredState);
        return new StarCardResponse(newStarredState);
    }

    @Override
    @Transactional
    public HideCardResponse toggleHide(Long userId, Long cardId) {
        log.info("Processing toggle hide for userId={}, cardId={}", userId, cardId);

        UserCardState state = userCardStateRepository
                .findByCardIdAndUserId(cardId, userId)
                .orElseGet(() -> {
                    log.info("No existing UserCardState for userId={}, cardId={} - creating new record with hidden / unhidden state", userId, cardId);
                    Card card = cardRepository.findById(cardId)
                            .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
                    return UserCardState.builder()
                            .card(card)
                            .deck(card.getDeck())
                            .topic(card.getTopic())
                            .user(user)
                            .build();
                });

        boolean newHiddenState = !state.getFlagsHidden();
        state.setFlagsHidden(newHiddenState);
        userCardStateRepository.save(state);

        log.info("Toggle hide successfully for userId={}, cardId={}, isHidden={}", userId, cardId, newHiddenState);
        return new HideCardResponse(newHiddenState);
    }
}
