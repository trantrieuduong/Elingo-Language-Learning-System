package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.service.DeckService;
import com.elingo.vocabulary.dto.response.TopicResponse;
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
@RequestMapping("/decks")
@Slf4j(topic = "DECK-CONTROLLER")
@RequiredArgsConstructor
@Tag(name = "Deck Controller")
public class DeckController {
    private final DeckService deckService;

    @GetMapping("/{deckId}/topics")
    @Operation(summary = "Get all topics belonging to a deck")
    public ApiResponse<List<TopicResponse>> getTopicsByDeck(
            @PathVariable Long deckId,
            @CurrentUserId Long currentUserId) {
        log.info("Get topics by deck request received: deckId={}, currentUserId={}", deckId, currentUserId);

        List<TopicResponse> topics = deckService.getTopicsByDeck(currentUserId, deckId);
        return ApiResponse.<List<TopicResponse>>builder()
                .success(true)
                .data(topics)
                .build();
    }
}