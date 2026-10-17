package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.dto.request.CreateCardRequest;
import com.elingo.vocabulary.dto.request.UpdateCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.elingo.common.annotation.IsAdmin;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@Tag(name = "Card Controller")
public class CardController {
    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a new user card (User only)")
    public ApiResponse<CardResponse> createUserCard(
            @RequestBody @Valid CreateCardRequest request,
            @CurrentUserId Long currentUserId) {
        CardResponse card = cardService.createUserCard(request, currentUserId);
        return ApiResponse.<CardResponse>builder()
                .success(true)
                .data(card)
                .build();
    }

    @PostMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new system card (Admin only)")
    public ApiResponse<CardResponse> createSystemCard(
            @RequestBody @Valid CreateCardRequest request) {
        CardResponse card = cardService.createSystemCard(request);
        return ApiResponse.<CardResponse>builder()
                .success(true)
                .data(card)
                .build();
    }

    @PutMapping("/{cardId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update a user card (User only)")
    public ApiResponse<CardResponse> updateUserCard(
            @PathVariable Long cardId,
            @RequestBody @Valid UpdateCardRequest request,
            @CurrentUserId Long currentUserId) {
        CardResponse card = cardService.updateUserCard(cardId, request, currentUserId);
        return ApiResponse.<CardResponse>builder()
                .success(true)
                .data(card)
                .build();
    }

    @PutMapping("/{cardId}/system")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a system card (Admin only)")
    public ApiResponse<CardResponse> updateSystemCard(
            @PathVariable Long cardId,
            @RequestBody @Valid UpdateCardRequest request) {
        CardResponse card = cardService.updateSystemCard(cardId, request);
        return ApiResponse.<CardResponse>builder()
                .success(true)
                .data(card)
                .build();
    }

    @DeleteMapping("/{cardId}")
    @Operation(summary = "Delete a card (Soft delete if published, Hard delete if draft)")
    public ApiResponse<Void> deleteCard(
            @PathVariable Long cardId,
            @CurrentUserId Long currentUserId,
            @IsAdmin boolean isAdmin) {
        cardService.deleteCard(cardId, currentUserId, isAdmin);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}