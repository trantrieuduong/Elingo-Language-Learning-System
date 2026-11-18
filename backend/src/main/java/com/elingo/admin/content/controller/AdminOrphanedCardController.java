package com.elingo.admin.content.controller;

import com.elingo.admin.content.dto.response.OrphanedCardCleanupResponse;
import com.elingo.admin.content.dto.response.OrphanedCardStatsResponse;
import com.elingo.admin.content.service.AdminOrphanedCardService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/orphaned-cards")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrphanedCardController {

    private final AdminOrphanedCardService adminOrphanedCardService;

    @GetMapping("/stats")
    public ApiResponse<OrphanedCardStatsResponse> getStats() {
        return ApiResponse.<OrphanedCardStatsResponse>builder()
                .success(true)
                .data(adminOrphanedCardService.getStats())
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<CardResponse>> getOrphanedCards(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<CardResponse>>builder()
                .success(true)
                .data(adminOrphanedCardService.getOrphanedCards(pageable))
                .build();
    }

    @PostMapping("/cleanup")
    public ApiResponse<OrphanedCardCleanupResponse> cleanupOrphanedCards() {
        return ApiResponse.<OrphanedCardCleanupResponse>builder()
                .success(true)
                .data(adminOrphanedCardService.cleanupOrphanedCards())
                .build();
    }
}
