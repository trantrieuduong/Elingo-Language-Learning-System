package com.elingo.admin.content.controller;

import com.elingo.admin.content.dto.request.CreateSystemCardRequest;
import com.elingo.admin.content.dto.request.UpdateSystemCardRequest;
import com.elingo.admin.content.service.AdminCardService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/admin/cards")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCardController {

    private final AdminCardService adminCardService;

    @GetMapping("/{cardId}")
    public ApiResponse<CardResponse> getCardById(@PathVariable Long cardId) {
        return ApiResponse.<CardResponse>builder()
                .success(true)
                .data(adminCardService.getCardById(cardId))
                .build();
    }

    @PostMapping
    public ApiResponse<CardResponse> createSystemCard(
            @Valid @RequestBody CreateSystemCardRequest request) {
        return ApiResponse.<CardResponse>builder()
                .success(true)
                .data(adminCardService.createSystemCard(request))
                .build();
    }

    @PutMapping("/{cardId}")
    public ApiResponse<CardResponse> updateSystemCard(
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateSystemCardRequest request) {
        return ApiResponse.<CardResponse>builder()
                .success(true)
                .data(adminCardService.updateSystemCard(cardId, request))
                .build();
    }

    @DeleteMapping("/{cardId}")
    public ApiResponse<Void> deleteSystemCard(@PathVariable Long cardId) {
        adminCardService.deleteSystemCard(cardId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
