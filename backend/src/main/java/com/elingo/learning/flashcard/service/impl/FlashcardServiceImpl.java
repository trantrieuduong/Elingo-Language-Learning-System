package com.elingo.learning.flashcard.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.response.QuizOptionResponse;


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
    private final UserCardStateRepository userCardStateRepository;
    private final SpacedRepetitionService spacedRepetitionService;

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
    public void toggleStar(Long userId, Long cardId) {
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
    }

    @Override
    @Transactional
    public void toggleHide(Long userId, Long cardId) {
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
    }
}
