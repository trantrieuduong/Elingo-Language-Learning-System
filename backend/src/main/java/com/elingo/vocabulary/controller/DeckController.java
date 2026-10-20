package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.request.DeckSearchRequest;
import com.elingo.vocabulary.dto.request.UserDeckPublicationRequest;
import com.elingo.vocabulary.dto.request.UserDeckRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.service.DeckService;
import com.elingo.vocabulary.dto.response.TopicResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "Get all topics belonging to a deck")
    public ApiResponse<List<TopicResponse>> getTopicsByDeck(
            @PathVariable Long deckId,
            @CurrentUserId Long currentUserId) {
        List<TopicResponse> topics = deckService.getTopicsByDeck(deckId, currentUserId);
        return ApiResponse.<List<TopicResponse>>builder()
                .success(true)
                .data(topics)
                .build();
    }

    @PostMapping
    @Operation(summary = "Create user deck")
    public ApiResponse<DeckResponse> createUserDeck(
            @Valid @RequestBody UserDeckRequest request,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<DeckResponse>builder()
                .success(true)
                .data(deckService.createUserDeck(currentUserId, request))
                .build();
    }

    @PutMapping("/{deckId}")
    @Operation(summary = "Update user deck")
    public ApiResponse<DeckResponse> updateUserDeck(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long deckId,
            @Valid @RequestBody UserDeckRequest request,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<DeckResponse>builder()
                .success(true)
                .data(deckService.updateUserDeck(currentUserId, deckId, request))
                .build();
    }

    @DeleteMapping("/{deckId}")
    @Operation(summary = "Hard delete user deck")
    public ApiResponse<Void> deleteUserDeck(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long deckId,
            @CurrentUserId Long currentUserId) {
        deckService.deleteUserDeck(currentUserId, deckId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PutMapping("/{deckId}/publication")
    public ApiResponse<DeckResponse> setUserDeckPublication(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long deckId,
            @Valid @RequestBody UserDeckPublicationRequest request,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<DeckResponse>builder()
                .success(true)
                .data(deckService.setUserDeckPublished(currentUserId, deckId, request.published()))
                .build();
    }
}