package com.elingo.learning.flashcard.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.response.ReviewCardResponse;
import com.elingo.learning.flashcard.entity.UserCardState;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.learning.flashcard.service.FlashcardService;
import com.elingo.learning.flashcard.service.SpacedRepetitionService;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.repository.CardRepository;
import org.springframework.data.domain.PageRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "FLASHCARD-SERVICE")
public class FlashcardServiceImpl implements FlashcardService {
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final UserCardStateRepository userCardStateRepository;
    private final SpacedRepetitionService spacedRepetitionService;
    private final CardMapper cardMapper;
    private final UserSubscriptionRepository userSubscriptionRepository;

    @Override
    @Transactional
    public void submitSrsReview(Long userId, Long cardId, SrsReviewRequest request) {
        int grade = request.grade();

        UserCardState state = getOrCreateState(userId, cardId);
        validateCardDeckAccess(state);

        spacedRepetitionService.calculateNextSRS(state, grade);
        userCardStateRepository.save(state);

        log.info("Srs review submitted cardId={} grade={} newInterval={} nextReview={}",
                cardId, grade, state.getSrsInterval(), state.getSrsNextReviewAt());

        // publishEvent FlashcardReviewedEvent
    }

    @Override
    @Transactional
    public void toggleStar(Long userId, Long cardId) {
        UserCardState state = getOrCreateState(userId, cardId);
        validateCardDeckAccess(state);

        boolean newStarredState = !state.getFlagsStarred();
        state.setFlagsStarred(newStarredState);
        userCardStateRepository.save(state);

        log.info("Star toggled cardId={} isStarred={}", cardId, newStarredState);
    }

    @Override
    @Transactional
    public void toggleHide(Long userId, Long cardId) {
        UserCardState state = getOrCreateState(userId, cardId);
        validateCardDeckAccess(state);

        boolean newHiddenState = !state.getFlagsHidden();
        state.setFlagsHidden(newHiddenState);
        userCardStateRepository.save(state);

        log.info("Hide toggled cardId={} isHidden={}", cardId, newHiddenState);
    }

    @Transactional
    private UserCardState getOrCreateState(Long userId, Long cardId) {
        return userCardStateRepository
                .findByCardIdAndUserId(cardId, userId)
                .orElseGet(() -> {
                    log.info("UserCardState record created cardId={}", cardId);
                    Card card = cardRepository.findById(cardId)
                            .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
                    UserCardState newState = UserCardState.builder()
                            .card(card)
                            .deck(card.getDeck())
                            .topic(card.getTopic())
                            .user(user)
                            .build();
                    return userCardStateRepository.save(newState);
                });
    }

    private void validateCardDeckAccess(UserCardState state) {
        Deck deck = state.getDeck();
        Long deckId = deck.getId();
        Long userId = state.getUser().getId();

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
        log.info("Deck access granted cardId={} deckId={}",
                state.getCard().getId(), deckId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewCardResponse> getCardsForReview(Long userId, Integer limit) {
        if (limit != null) {
            if (limit > 200) {
                throw new AppException(AppError.MAX_REVIEW_CARD_LIMIT_INVALID);
            }
            if (limit < 1) {
                throw new AppException(AppError.MIN_REVIEW_CARD_LIMIT_INVALID);
            }
        } else {
            limit = 100;
        }

        List<UserCardState> states = userCardStateRepository.findCardsForReview(
                userId, LocalDateTime.now(), PageRequest.of(0, limit));
        if (states.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> cardIds = states.stream()
                .map(state -> state.getCard().getId())
                .toList();
        List<Card> cards = cardRepository.findAllWithPhoneticsByIdIn(cardIds);
        Map<Long, Card> cardMap = cards.stream().collect(Collectors.toMap(Card::getId, Function.identity()));
        log.info("Review cards fetched count={} limit={}", cards.size(), limit);
        return states.stream().map(
                state -> new ReviewCardResponse(
                        cardMapper.toCardResponse(cardMap.get(state.getCard().getId())),
                        state.getSrsNextReviewAt(),
                        state.getFlagsStarred(),
                        state.getFlagsHidden())
        ).toList();
    }
}
