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
        log.info("Processing Srs review submission for userId={}, cardId={}, grade={}", userId, cardId, grade);

        UserCardState state = getOrCreateState(userId, cardId);
        validateCardDeckAccess(state);

        spacedRepetitionService.calculateNextSRS(state, grade);
        userCardStateRepository.save(state);

        log.info("Submit Srs review successfully for userId={}, cardId={}, grade={}, newInterval={}, nextReview={}",
                userId, cardId, grade, state.getSrsInterval(), state.getSrsNextReviewAt());

        // publishEvent FlashcardReviewedEvent
    }

    @Override
    @Transactional
    public void toggleStar(Long userId, Long cardId) {
        log.info("Processing toggle star for userId={}, cardId={}", userId, cardId);

        UserCardState state = getOrCreateState(userId, cardId);
        validateCardDeckAccess(state);

        boolean newStarredState = !state.getFlagsStarred();
        state.setFlagsStarred(newStarredState);
        userCardStateRepository.save(state);

        log.info("Toggle star successfully for userId={}, cardId={}, isStarred={}", userId, cardId, newStarredState);
    }

    @Override
    @Transactional
    public void toggleHide(Long userId, Long cardId) {
        log.info("Processing toggle hide for userId={}, cardId={}", userId, cardId);

        UserCardState state = getOrCreateState(userId, cardId);
        validateCardDeckAccess(state);

        boolean newHiddenState = !state.getFlagsHidden();
        state.setFlagsHidden(newHiddenState);
        userCardStateRepository.save(state);

        log.info("Toggle hide successfully for userId={}, cardId={}, isHidden={}", userId, cardId, newHiddenState);
    }

    @Transactional
    private UserCardState getOrCreateState(Long userId, Long cardId) {
        return userCardStateRepository
                .findByCardIdAndUserId(cardId, userId)
                .orElseGet(() -> {
                    log.info("No existing UserCardState for userId={}, cardId={} - creating new record", userId, cardId);
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
        var deck = state.getDeck();
        Long deckId = deck.getId();
        Long userId = state.getUser().getId();

        if (deck.getStatus() != DeckStatus.PUBLISHED) {
            log.warn("Card belongs to unpublished deck: cardId={}, deckId={}, status={}",
                    state.getCard().getId(), deckId, deck.getStatus());
            throw new AppException(AppError.DECK_NOT_FOUND);
        }

        if (deck.getIsPremium()) {
            boolean hasActiveSubscription = userSubscriptionRepository
                    .existsByUserIdAndStatusAndEndAtAfter(userId, SubscriptionStatus.ACTIVE, LocalDateTime.now());
            if (!hasActiveSubscription) {
                log.warn("User {} tried to access premium deck {} without active subscription",
                        userId, deckId);
                throw new AppException(AppError.DECK_PREMIUM_REQUIRED);
            }
        }
        log.info("Deck access granted: cardId={}, deckId={}, userId={}",
                state.getCard().getId(), deckId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewCardResponse> getCardsForReview(Long userId, Integer limit) {
        log.info("Fetching cards for review for userId={}, limit={}", userId, limit);
        limit = limit != null && limit > 0 ? limit : 100;

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
        return states.stream().map(
                state -> new ReviewCardResponse(
                        cardMapper.toCardResponse(cardMap.get(state.getCard().getId())),
                        state.getSrsNextReviewAt(),
                        state.getFlagsStarred(),
                        state.getFlagsHidden())
        ).toList();
    }
}
