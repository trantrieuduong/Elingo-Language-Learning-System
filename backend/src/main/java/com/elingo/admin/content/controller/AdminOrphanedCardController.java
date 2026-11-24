package com.elingo.admin.content.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.vocabulary.dto.response.OrphanedCardCleanupResponse;
import com.elingo.vocabulary.dto.response.OrphanedCardStatsResponse;
import com.elingo.admin.content.service.AdminOrphanedCardService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AdminApiPaths.ORPHANED_CARDS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Orphaned Cards",
        description = "Management endpoints for inspecting and cleaning up orphaned flashcards"
)
public class AdminOrphanedCardController {

    private final AdminOrphanedCardService adminOrphanedCardService;

    @GetMapping("/stats")
    @Operation(
            summary = "Get orphaned cards statistics",
            description = "Retrieves aggregated counts of total orphaned cards, cards safe to delete (no student review history), and cards retained due to active learning progress."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Orphaned cards statistics retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "OrphanedCardStatsSuccessResponse",
                            summary = "Sample orphaned card stats response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "totalOrphanedCards": 120,
                                        "safeToDeleteCards": 45,
                                        "retainedInStudyCards": 75
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<OrphanedCardStatsResponse> getStats() {
        return ApiResponse.<OrphanedCardStatsResponse>builder()
                .success(true)
                .data(adminOrphanedCardService.getStats())
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Get paginated orphaned cards",
            description = "Retrieves a paginated list of detached cards that currently have no associated topic."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Orphaned cards list retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetOrphanedCardsSuccessResponse",
                            summary = "Sample orphaned cards page response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "content": [
                                          {
                                            "id": 505,
                                            "order": 1,
                                            "term": "obsolete",
                                            "pos": "adjective",
                                            "translation": "lỗi thời, không còn sử dụng",
                                            "explanationVi": "Không còn được dùng vì đã có cái mới thay thế",
                                            "explanationEn": "No longer produced or used; out of date",
                                            "examplesVi": "Công nghệ này đã trở nên lỗi thời.",
                                            "examplesEn": "This technology has become obsolete.",
                                            "imageUrl": "https://cdn.elingo.pro/uploads/cards/obsolete.png",
                                            "phonetics": [
                                              {
                                                "text": "/ˌɒb.səlˈiːt/",
                                                "audioUrl": "https://cdn.elingo.pro/uploads/cards/obsolete_uk.mp3",
                                                "locale": "UK"
                                              }
                                            ]
                                          }
                                        ],
                                        "page": 1,
                                        "size": 20,
                                        "totalElements": 1,
                                        "totalPages": 1,
                                        "isLast": true
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<PageResponse<CardResponse>> getOrphanedCards(
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<CardResponse>>builder()
                .success(true)
                .data(adminOrphanedCardService.getOrphanedCards(pageable))
                .build();
    }

    @PostMapping("/cleanup")
    @Operation(
            summary = "Trigger cleanup of safe orphaned cards",
            description = "Executes batch deletion for orphaned cards that have zero learning records in user_card_states, along with their associated media assets in Cloudflare R2 storage."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Cleanup executed successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "CleanupOrphanedCardsSuccessResponse",
                            summary = "Sample cleanup outcome response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "scannedCount": 45,
                                        "deletedCardCount": 45,
                                        "deletedFileCount": 38
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<OrphanedCardCleanupResponse> cleanupOrphanedCards() {
        return ApiResponse.<OrphanedCardCleanupResponse>builder()
                .success(true)
                .data(adminOrphanedCardService.cleanupOrphanedCards())
                .build();
    }
}
