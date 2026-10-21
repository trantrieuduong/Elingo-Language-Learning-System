package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.dto.request.CreateUserCardRequest;
import com.elingo.vocabulary.dto.request.UpdateUserCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@Tag(name = "Card Controller")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @Operation(summary = "Create user card in topic")
    public ApiResponse<CardResponse> createUserCard(
            @Valid @RequestBody CreateUserCardRequest request,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<CardResponse>builder()
                .success(true)
                .data(cardService.createUserCard(currentUserId, request))
                .build();
    }

    @PutMapping("/{cardId}")
    @Operation(summary = "Update user card")
    public ApiResponse<CardResponse> updateUserCard(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long cardId,
            @Valid @RequestBody UpdateUserCardRequest request,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<CardResponse>builder()
                .success(true)
                .data(cardService.updateUserCard(currentUserId, cardId, request))
                .build();
    }

    @DeleteMapping("/{cardId}")
    @Operation(summary = "Delete vocabulary card")
    public ApiResponse<Void> deleteUserCard(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long cardId,
            @CurrentUserId Long currentUserId) {
        cardService.deleteUserCard(currentUserId, cardId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
