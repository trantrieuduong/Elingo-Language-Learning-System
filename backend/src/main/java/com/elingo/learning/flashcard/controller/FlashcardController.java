package com.elingo.learning.flashcard.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.learning.flashcard.dto.request.HiddenCardRequest;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.request.StarCardRequest;

import com.elingo.learning.flashcard.service.CardStateService;
import com.elingo.vocabulary.dto.response.CardResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flashcards")
@RequiredArgsConstructor
@Tag(name = "Flashcard Controller")
public class FlashcardController {

    private final CardStateService cardStateService;

    @PostMapping("/{cardId}/srs")
    public ApiResponse<Void> submitSrsReview(
            @PathVariable Long cardId,
            @Valid @RequestBody SrsReviewRequest request,
            @CurrentUserId Long currentUserId) {
        cardStateService.submitReview(currentUserId, cardId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PutMapping("/stars")
    public ApiResponse<Void> starCard(
            @Valid @RequestBody StarCardRequest request,
            @CurrentUserId Long currentUserId) {
        cardStateService.setStarred(currentUserId, request.cardId(), request.starred());
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PutMapping("/hidden")
    public ApiResponse<Void> hideCard(
            @Valid @RequestBody HiddenCardRequest request,
            @CurrentUserId Long currentUserId) {
        cardStateService.setHidden(currentUserId, request.cardId(), request.hidden());
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @GetMapping("/due")
    public ApiResponse<List<CardResponse>> getDueCards(
            @Min(value = 1, message = "INVALID_REQUEST")
            @Max(value = 100, message = "INVALID_REQUEST")
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<List<CardResponse>>builder()
                .success(true)
                .data(cardStateService.getDueCards(currentUserId, limit))
                .build();
    }
}
