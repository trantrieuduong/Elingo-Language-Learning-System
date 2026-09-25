package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.learning.flashcard.dto.response.QuizOptionResponse;
import com.elingo.vocabulary.service.TopicService;
import com.elingo.vocabulary.dto.response.CardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/topics")
@Slf4j(topic = "TOPIC-CONTROLLER")
@RequiredArgsConstructor
@Tag(name = "Topic Controller")
public class TopicController {
    private final TopicService topicService;

    @GetMapping("/{topicId}/cards")
    @Operation(summary = "Get all cards belonging to a topic")
    public ApiResponse<List<CardResponse>> getCardsByTopic(
            @PathVariable Long topicId,
            @CurrentUserId Long currentUserId) {
        log.info("Get cards by topic request received: topicId={}, currentUserId={}", topicId, currentUserId);

        List<CardResponse> cards = topicService.getCardsByTopic(currentUserId, topicId);
        return ApiResponse.<List<CardResponse>>builder()
                .success(true)
                .data(cards)
                .build();
    }

    @GetMapping("/{topicId}/cards/{cardId}/quiz-options")
    @Operation(summary = "Generate 4-option quiz for a card (QUIZ tab)")
    public ApiResponse<List<QuizOptionResponse>> getQuizOptions(
            @PathVariable Long topicId,
            @PathVariable Long cardId,
            @CurrentUserId Long currentUserId) {
        log.info("Quiz options request: topicId={}, cardId={}, userId={}", topicId, cardId, currentUserId);

        List<QuizOptionResponse> quiz = topicService.getQuizOptions(currentUserId, topicId, cardId);
        return ApiResponse.<List<QuizOptionResponse>>builder()
                .success(true)
                .data(quiz)
                .build();
    }
}
