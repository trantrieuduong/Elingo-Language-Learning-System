package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.request.DeckSearchRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.service.DeckService;
import com.elingo.vocabulary.dto.response.TopicResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/decks")
@RequiredArgsConstructor
@Tag(name = "Deck Controller")
public class DeckController {

    private final DeckService deckService;

    @GetMapping
    @Operation(summary = "Get all published decks with optional filters and pagination")
    public ApiResponse<PageResponse<DeckResponse>> getPublishedDecks(
            @ModelAttribute @Valid DeckSearchRequest request,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @CurrentUserId Long currentUserId
    ) {
        PageResponse<DeckResponse> decks = deckService.getPublishedDecks(request, pageable, currentUserId);
        return ApiResponse.<PageResponse<DeckResponse>>builder()
                .success(true)
                .data(decks)
                .build();
    }

    @GetMapping("/{deckId}/topics")
    @Operation(summary = "Get all topics belonging to a deck (by slug or ID)")
    public ApiResponse<List<TopicResponse>> getTopicsByDeck(
            @PathVariable Long deckId,
            @CurrentUserId Long currentUserId) {
        List<TopicResponse> topics = deckService.getTopicsByDeck(deckId, currentUserId);
        return ApiResponse.<List<TopicResponse>>builder()
                .success(true)
                .data(topics)
                .build();
    }
}