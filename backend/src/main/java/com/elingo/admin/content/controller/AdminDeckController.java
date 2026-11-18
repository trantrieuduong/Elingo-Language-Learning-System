package com.elingo.admin.content.controller;

import com.elingo.admin.content.dto.request.*;
import com.elingo.admin.content.service.AdminDeckService;
import com.elingo.admin.content.service.AdminTopicService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/decks")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDeckController {

    private final AdminDeckService adminDeckService;
    private final AdminTopicService adminTopicService;

    @GetMapping
    public ApiResponse<PageResponse<DeckResponse>> searchDecks(
            @Valid @ModelAttribute AdminDeckSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<DeckResponse>>builder()
                .success(true)
                .data(adminDeckService.searchDecks(request, pageable))
                .build();
    }

    @GetMapping("/{deckId}")
    public ApiResponse<DeckResponse> getDeckById(@PathVariable Long deckId) {
        return ApiResponse.<DeckResponse>builder()
                .success(true)
                .data(adminDeckService.getDeckById(deckId))
                .build();
    }

    @GetMapping("/{deckId}/topics")
    public ApiResponse<List<TopicResponse>> getTopicsByDeckId(@PathVariable Long deckId) {
        return ApiResponse.<List<TopicResponse>>builder()
                .success(true)
                .data(adminTopicService.getTopicsByDeckId(deckId))
                .build();
    }

    @PatchMapping("/{deckId}/topics/orders")
    public ApiResponse<Void> reorderSystemTopics(
            @PathVariable Long deckId,
            @Valid @RequestBody ReorderItemRequest request) {
        adminTopicService.reorderSystemTopics(deckId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping
    public ApiResponse<DeckResponse> createSystemDeck(@Valid @RequestBody SystemDeckRequest request) {
        return ApiResponse.<DeckResponse>builder()
                .success(true)
                .data(adminDeckService.createSystemDeck(request))
                .build();
    }

    @PutMapping("/{deckId}")
    public ApiResponse<DeckResponse> updateSystemDeck(
            @PathVariable Long deckId,
            @Valid @RequestBody SystemDeckRequest request) {
        return ApiResponse.<DeckResponse>builder()
                .success(true)
                .data(adminDeckService.updateSystemDeck(deckId, request))
                .build();
    }

    @PatchMapping("/{deckId}/status")
    public ApiResponse<DeckResponse> updateDeckStatus(
            @PathVariable Long deckId,
            @Valid @RequestBody UpdateDeckStatusRequest request) {
        return ApiResponse.<DeckResponse>builder()
                .success(true)
                .data(adminDeckService.updateStatus(deckId, request))
                .build();
    }

    @DeleteMapping("/{deckId}")
    public ApiResponse<Void> deleteUserDeck(
            @PathVariable Long deckId,
            @RequestParam(required = false, defaultValue = "Policy violation") String reason) {
        adminDeckService.deleteUserDeck(deckId, reason);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
