package com.elingo.learning.flashcard.service.impl;

import com.elingo.common.event.XpGainedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.service.RedisService;
import com.elingo.gamification.entity.XpSource;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.response.CardLearningState;
import com.elingo.learning.flashcard.entity.UserCardState;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.learning.flashcard.service.CardStateService;
import com.elingo.learning.flashcard.service.SpacedRepetitionService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.service.DeckAccessPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CARD-STATE-SERVICE")
public class CardStateServiceImpl implements CardStateService {

    private static final Duration REVIEW_GUARD_TTL = Duration.ofSeconds(2);

    private final UserCardStateRepository stateRepository;
    private final RedisService redisService;
    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final DeckAccessPolicy accessPolicy;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;
    private final SpacedRepetitionService spacedRepetitionService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void submitReview(Long userId, Long cardId, SrsReviewRequest request) {
        guardDuplicateSubmit(userId, cardId);
        UserCardState state = getOrCreateLocked(userId, cardId);

        spacedRepetitionService.calculateNextSRS(state, request.grade());

        eventPublisher.publishEvent(new XpGainedEvent(userId, cardId, XpSource.FLASHCARD_REVIEW, LocalDateTime.now()));

        log.info("Review submitted cardId={} grade={} nextReviewAt={}",
                cardId, request.grade(), state.getSrsNextReviewAt());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, CardLearningState> getStates(Long userId, Collection<Long> cardIds) {
        if (userId == null || cardIds.isEmpty()) return Map.of();
        return stateRepository.findStateViews(userId, cardIds).stream()
                .collect(Collectors.toMap(
                        UserCardStateRepository.CardStateView::getCardId,
                        cardStateView -> CardLearningState.of(
                                cardStateView.getSrsLastGrade(),
                                cardStateView.getFlagsStarred(),
                                cardStateView.getFlagsHidden()
                        ))
                );
    }

    @Override
    @Transactional
    public void setStarred(Long userId, Long cardId, boolean starred) {
        UserCardState state = getOrCreateLocked(userId, cardId);
        state.setFlagsStarred(starred);
        log.info("Card star changed cardId={} starred={}", cardId, starred);
    }

    @Override
    @Transactional
    public void setHidden(Long userId, Long cardId, boolean hidden) {
        UserCardState state = getOrCreateLocked(userId, cardId);
        state.setFlagsHidden(hidden);
        log.info("Card hidden changed cardId={} hidden={}", cardId, hidden);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> getDueCards(Long userId, int limit) {
        return stateRepository.findDue(userId, LocalDateTime.now(), PageRequest.of(0, limit))
                .stream().map(state -> cardMapper.toCardResponse(
                        state.getCard(),
                        CardLearningState.of(
                                state.getSrsLastGrade(),
                                state.getFlagsStarred(),
                                state.getFlagsHidden()
                        )
                )).toList();
    }

    private void guardDuplicateSubmit(Long userId, Long cardId) {
        String key = "srs:card-review-guard:%d:%d".formatted(userId, cardId);
        long count = redisService.incrementWithTtl(key, REVIEW_GUARD_TTL);
        if (count > 1) {
            throw new AppException(AppError.RATE_LIMIT_EXCEEDED);
        }
    }

    private UserCardState getOrCreateLocked(Long userId, Long cardId) {
        return stateRepository.findForUpdate(userId, cardId)
                .orElseGet(() -> createState(userId, cardId));
    }

    private UserCardState createState(Long userId, Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));

        if (card.getDeck() == null) throw new AppException(AppError.CARD_NOT_AVAILABLE);

        DeckRepository.DeckAccessView deck = deckRepository.findAccessViewById(card.getDeck().getId())
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));
        accessPolicy.assertCanLearn(deck, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        return stateRepository.save(UserCardState.builder()
                        .user(user)
                        .card(card)
                .build());
    }
}
