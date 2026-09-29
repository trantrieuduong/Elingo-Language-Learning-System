package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
}
