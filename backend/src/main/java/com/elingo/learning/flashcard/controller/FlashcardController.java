package com.elingo.learning.flashcard.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;

import com.elingo.learning.flashcard.service.FlashcardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flashcards")
@Slf4j(topic = "FLASHCARD-CONTROLLER")
@RequiredArgsConstructor
@Tag(name = "Flashcard Controller")
public class FlashcardController {
    private final FlashcardService flashcardService;

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
    public ApiResponse<Void> toggleStar(
            @PathVariable Long cardId,
            @CurrentUserId Long currentUserId) {
        log.info("Toggle star request received: cardId={}, userId={}", cardId, currentUserId);

        flashcardService.toggleStar(currentUserId, cardId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PatchMapping("/{cardId}/hidden")
    @Operation(summary = "Toggle hidden state for user flashcard")
    public ApiResponse<Void> toggleHidden(
            @PathVariable Long cardId,
            @CurrentUserId Long currentUserId) {
        log.info("Toggle hidden request received: cardId={}, userId={}", cardId, currentUserId);

        flashcardService.toggleHide(currentUserId, cardId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
