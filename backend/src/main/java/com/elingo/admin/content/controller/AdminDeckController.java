package com.elingo.admin.content.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.vocabulary.dto.request.*;
import com.elingo.admin.content.service.AdminDeckService;
import com.elingo.admin.content.service.AdminTopicService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AdminApiPaths.DECKS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Decks",
        description = "Management endpoints for system and user decks in the admin console"
)
public class AdminDeckController {

    private final AdminDeckService adminDeckService;
    private final AdminTopicService adminTopicService;

    @GetMapping
    @Operation(
            summary = "Search and filter decks",
            description = "Retrieves a paginated list of decks filtered by keyword, owner type (SYSTEM or USER), publication status, premium status, tags, and CEFR levels."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Decks retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "SearchDecksSuccessResponse",
                            summary = "Sample deck list response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "content": [
                                          {
                                            "id": 1,
                                            "title": "Oxford 3000 Words",
                                            "slug": "oxford-3000-words",
                                            "description": "Essential vocabulary for English learners",
                                            "isPremium": false,
                                            "coverImageUrl": "https://cdn.elingo.pro/uploads/decks/oxford3000.png",
                                            "status": "PUBLIC",
                                            "ownerType": "SYSTEM",
                                            "ownerId": 1,
                                            "topicCount": 15,
                                            "cardCount": 3000,
                                            "learnedCardCount": 0,
                                            "publishedAt": "2026-01-10T10:00:00",
                                            "tags": [
                                              { "id": 1, "label": "General English" }
                                            ],
                                            "cefrLevels": [
                                              { "id": 2, "label": "B1" }
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
    public ApiResponse<PageResponse<DeckResponse>> searchDecks(
            @Valid @ParameterObject AdminDeckSearchRequest request,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<DeckResponse>>builder()
                .success(true)
                .data(adminDeckService.searchDecks(request, pageable))
                .build();
    }

    @GetMapping("/{deckId}")
    @Operation(
            summary = "Get deck details by ID",
            description = "Retrieves full metadata of a specific deck by its primary key identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Deck details retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetDeckByIdSuccessResponse",
                            summary = "Sample deck detail response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "title": "Oxford 3000 Words",
                                        "slug": "oxford-3000-words",
                                        "description": "Essential vocabulary for English learners",
                                        "isPremium": false,
                                        "coverImageUrl": "https://cdn.elingo.pro/uploads/decks/oxford3000.png",
                                        "status": "PUBLIC",
                                        "ownerType": "SYSTEM",
                                        "ownerId": 1,
                                        "topicCount": 15,
                                        "cardCount": 3000,
                                        "learnedCardCount": 0,
                                        "publishedAt": "2026-01-10T10:00:00",
                                        "tags": [
                                          { "id": 1, "label": "General English" }
                                        ],
                                        "cefrLevels": [
                                          { "id": 2, "label": "B1" }
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<DeckResponse> getDeckById(
            @Parameter(description = "Identifier of the deck", example = "1", required = true)
            @PathVariable Long deckId) {
        return ApiResponse.<DeckResponse>builder()
                .success(true)
                .data(adminDeckService.getDeckById(deckId))
                .build();
    }

    @GetMapping("/{deckId}/topics")
    @Operation(
            summary = "Get topics of a deck",
            description = "Retrieves all topics belonging to the specified deck, ordered by display order."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Topics retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetTopicsByDeckIdSuccessResponse",
                            summary = "Sample topics list response",
                            value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "id": 10,
                                          "name": "Unit 1: Meeting New People",
                                          "slug": "unit-1-meeting-new-people",
                                          "order": 1,
                                          "cardCount": 20,
                                          "learnedCardCount": 0
                                        },
                                        {
                                          "id": 11,
                                          "name": "Unit 2: Daily Routines",
                                          "slug": "unit-2-daily-routines",
                                          "order": 2,
                                          "cardCount": 25,
                                          "learnedCardCount": 0
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<List<TopicResponse>> getTopicsByDeckId(
            @Parameter(description = "Identifier of the deck", example = "1", required = true)
            @PathVariable Long deckId) {
        return ApiResponse.<List<TopicResponse>>builder()
                .success(true)
                .data(adminTopicService.getTopicsByDeckId(deckId))
                .build();
    }

    @PatchMapping("/{deckId}/topics/orders")
    @Operation(
            summary = "Reorder system topics in a deck",
            description = "Updates the sequential display order of topics inside a system deck."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "List of topic IDs in desired display order",
            content = @Content(
                    examples = @ExampleObject(
                            name = "ReorderTopicsRequestExample",
                            value = """
                                    {
                                      "orderedIds": [11, 10]
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Topics reordered successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "ReorderTopicsSuccessResponse",
                            value = """
                                    {
                                      "success": true
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<Void> reorderSystemTopics(
            @Parameter(description = "Identifier of the deck", example = "1", required = true)
            @PathVariable Long deckId,
            @Valid @RequestBody ReorderItemRequest request) {
        adminTopicService.reorderSystemTopics(deckId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping
    @Operation(
            summary = "Create a new system deck",
            description = "Creates a new system-owned deck with verified cover image and optional metadata."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "System deck creation payload",
            content = @Content(
                    examples = @ExampleObject(
                            name = "CreateSystemDeckRequestExample",
                            value = """
                                    {
                                      "title": "Business English Communication",
                                      "description": "Professional phrases and terminology for meetings and presentations",
                                      "coverImageUrl": "verified/decks/business.png",
                                      "isPremium": true,
                                      "tagIds": [1, 2],
                                      "cefrLevelIds": [3]
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "System deck created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "CreateSystemDeckSuccessResponse",
                            summary = "Sample created deck response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 2,
                                        "title": "Business English Communication",
                                        "slug": "business-english-communication",
                                        "description": "Professional phrases and terminology for meetings and presentations",
                                        "isPremium": true,
                                        "coverImageUrl": "https://cdn.elingo.pro/uploads/decks/business.png",
                                        "status": "PUBLIC",
                                        "ownerType": "SYSTEM",
                                        "ownerId": 1,
                                        "topicCount": 0,
                                        "cardCount": 0,
                                        "learnedCardCount": 0,
                                        "publishedAt": "2026-03-01T08:00:00",
                                        "tags": [
                                          { "id": 1, "label": "General English" }
                                        ],
                                        "cefrLevels": [
                                          { "id": 3, "label": "B2" }
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<DeckResponse> createSystemDeck(@Valid @RequestBody SystemDeckRequest request) {
        return ApiResponse.<DeckResponse>builder()
                .success(true)
                .data(adminDeckService.createSystemDeck(request))
                .build();
    }

    @PutMapping("/{deckId}")
    @Operation(
            summary = "Update an existing system deck",
            description = "Modifies metadata, cover image, tags, and CEFR levels of an existing system deck."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "System deck update payload",
            content = @Content(
                    examples = @ExampleObject(
                            name = "UpdateSystemDeckRequestExample",
                            value = """
                                    {
                                      "title": "Business English Communication (Revised)",
                                      "description": "Updated terminology for modern workplace communication",
                                      "coverImageUrl": "uploads/decks/business.png",
                                      "isPremium": false,
                                      "tagIds": [1],
                                      "cefrLevelIds": [3, 4]
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "System deck updated successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UpdateSystemDeckSuccessResponse",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 2,
                                        "title": "Business English Communication (Revised)",
                                        "slug": "business-english-communication",
                                        "description": "Updated terminology for modern workplace communication",
                                        "isPremium": false,
                                        "coverImageUrl": "https://cdn.elingo.pro/uploads/decks/business.png",
                                        "status": "PUBLIC",
                                        "ownerType": "SYSTEM",
                                        "ownerId": 1,
                                        "topicCount": 3,
                                        "cardCount": 45,
                                        "learnedCardCount": 0,
                                        "publishedAt": "2026-03-01T08:00:00",
                                        "tags": [
                                          { "id": 1, "label": "General English" }
                                        ],
                                        "cefrLevels": [
                                          { "id": 3, "label": "B2" },
                                          { "id": 4, "label": "C1" }
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<DeckResponse> updateSystemDeck(
            @Parameter(description = "Identifier of the deck to update", example = "2", required = true)
            @PathVariable Long deckId,
            @Valid @RequestBody SystemDeckRequest request) {
        return ApiResponse.<DeckResponse>builder()
                .success(true)
                .data(adminDeckService.updateSystemDeck(deckId, request))
                .build();
    }

    @PatchMapping("/{deckId}/status")
    @Operation(
            summary = "Update deck status",
            description = "Transitions deck visibility status among PUBLIC, PRIVATE, and ARCHIVED."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Status update payload",
            content = @Content(
                    examples = @ExampleObject(
                            name = "UpdateDeckStatusRequestExample",
                            value = """
                                    {
                                      "status": "ARCHIVED"
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Deck status updated successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UpdateDeckStatusSuccessResponse",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 2,
                                        "title": "Business English Communication",
                                        "slug": "business-english-communication",
                                        "status": "ARCHIVED",
                                        "ownerType": "SYSTEM"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<DeckResponse> updateDeckStatus(
            @Parameter(description = "Identifier of the deck", example = "2", required = true)
            @PathVariable Long deckId,
            @Valid @RequestBody UpdateDeckStatusRequest request) {
        return ApiResponse.<DeckResponse>builder()
                .success(true)
                .data(adminDeckService.updateStatus(deckId, request))
                .build();
    }

    @DeleteMapping("/{deckId}")
    @Operation(
            summary = "Delete user deck by admin",
            description = "Administratively deletes a user-created deck with an audit reason, safely preserving cards in orphaned state if students have active study progress."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User deck deleted successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "DeleteUserDeckSuccessResponse",
                            value = """
                                    {
                                      "success": true
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<Void> deleteUserDeck(
            @Parameter(description = "Identifier of the user deck to delete", example = "5", required = true)
            @PathVariable Long deckId,
            @Parameter(description = "Administrative reason for deck deletion", example = "Inappropriate content violation")
            @RequestParam(required = false, defaultValue = "Policy violation") String reason) {
        adminDeckService.deleteUserDeck(deckId, reason);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
