package com.elingo.learning.flashcard.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.response.QuizResponse;
import com.elingo.learning.flashcard.dto.response.StarCardResponse;
import com.elingo.learning.flashcard.dto.response.HideCardResponse;
import com.elingo.learning.flashcard.service.FlashcardService;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flashcard")
@Slf4j(topic = "FLASHCARD-CONTROLLER")
@RequiredArgsConstructor
@Tag(name = "Flashcard Controller")
public class FlashcardController {
    private final FlashcardService flashcardService;

    @GetMapping("/decks/{deckId}/topics")
    @Operation(summary = "Get all topics belonging to a deck")
    public ApiResponse<List<TopicResponse>> getTopicsByDeck(
            @PathVariable Long deckId,
            @CurrentUserId Long currentUserId) {
        log.info("Get topics by deck request received: deckId={}, currentUserId={}", deckId, currentUserId);

        List<TopicResponse> topics = flashcardService.getTopicsByDeck(currentUserId, deckId);
        return ApiResponse.<List<TopicResponse>>builder()
                .success(true)
                .data(topics)
                .build();
    }

    @GetMapping("/topics/{topicId}/cards")
    @Operation(summary = "Get all cards belonging to a topic")
    public ApiResponse<List<CardResponse>> getCardsByTopic(
            @PathVariable Long topicId,
            @CurrentUserId Long currentUserId) {
        log.info("Get cards by topic request received: topicId={}, currentUserId={}", topicId, currentUserId);

        List<CardResponse> cards = flashcardService.getCardsByTopic(currentUserId, topicId);
        return ApiResponse.<List<CardResponse>>builder()
                .success(true)
                .data(cards)
                .build();
    }

    @GetMapping("/topics/{topicId}/cards/{cardId}/quiz-options")
    @Operation(summary = "Generate 4-option quiz for a card (QUIZ tab)")
    public ApiResponse<QuizResponse> getQuizOptions(
            @PathVariable Long topicId,
            @PathVariable Long cardId,
            @CurrentUserId Long currentUserId) {
        log.info("Quiz options request: topicId={}, cardId={}, userId={}", topicId, cardId, currentUserId);

        QuizResponse quiz = flashcardService.getQuizOptions(currentUserId, topicId, cardId);
        return ApiResponse.<QuizResponse>builder()
                .success(true)
                .data(quiz)
                .build();
    }

    @PatchMapping("/{cardId}/srs")
    @Operation(summary = "Submit SM-2 review grade for a card (0=Again, 1=Hard, 2=Good, 3=Easy)")
    public ApiResponse<Void> submitSrsReview(
            @PathVariable Long cardId,
            @Valid @RequestBody SrsReviewRequest request,
            @CurrentUserId Long currentUserId) {
        log.info("SRS review request received: cardId={}, grade={}, userId={}",
                cardId, request.grade(), currentUserId);

        flashcardService.submitSrsReview(currentUserId, cardId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PatchMapping("/{cardId}/stars")
    @Operation(summary = "Toggle star state for user flashcard")
    public ApiResponse<StarCardResponse> toggleStar(
            @PathVariable Long cardId,
            @CurrentUserId Long currentUserId) {
        log.info("Toggle star request received: cardId={}, userId={}", cardId, currentUserId);

        StarCardResponse response = flashcardService.toggleStar(currentUserId, cardId);
        return ApiResponse.<StarCardResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    @PatchMapping("/{cardId}/hidden")
    @Operation(summary = "Toggle hidden state for user flashcard")
    public ApiResponse<HideCardResponse> toggleHidden(
            @PathVariable Long cardId,
            @CurrentUserId Long currentUserId) {
        log.info("Toggle hidden request received: cardId={}, userId={}", cardId, currentUserId);

        HideCardResponse response = flashcardService.toggleHide(currentUserId, cardId);
        return ApiResponse.<HideCardResponse>builder()
                .success(true)
                .data(response)
                .build();
    }
}
