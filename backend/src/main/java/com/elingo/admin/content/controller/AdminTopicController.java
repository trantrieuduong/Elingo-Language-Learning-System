package com.elingo.admin.content.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.vocabulary.dto.request.CreateSystemTopicRequest;
import com.elingo.vocabulary.dto.request.ReorderItemRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemTopicRequest;
import com.elingo.admin.content.service.AdminCardService;
import com.elingo.admin.content.service.AdminTopicService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AdminApiPaths.TOPICS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Topics",
        description = "Management endpoints for system topics and cards ordering in the admin console"
)
public class AdminTopicController {

    private final AdminTopicService adminTopicService;
    private final AdminCardService adminCardService;

    @PostMapping
    @Operation(
            summary = "Create a new system topic",
            description = "Creates a new topic inside an existing system deck."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "System topic creation payload",
            content = @Content(
                    examples = @ExampleObject(
                            name = "CreateSystemTopicRequestExample",
                            value = """
                                    {
                                      "deckId": 1,
                                      "name": "Unit 3: Travelling and Airports"
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "System topic created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "CreateSystemTopicSuccessResponse",
                            summary = "Sample created topic response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 12,
                                        "name": "Unit 3: Travelling and Airports",
                                        "slug": "unit-3-travelling-and-airports",
                                        "order": 3,
                                        "cardCount": 0,
                                        "learnedCardCount": 0
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<TopicResponse> createSystemTopic(
            @Valid @RequestBody CreateSystemTopicRequest request) {
        return ApiResponse.<TopicResponse>builder()
                .success(true)
                .data(adminTopicService.createSystemTopic(request))
                .build();
    }

    @PutMapping("/{topicId}")
    @Operation(
            summary = "Update an existing system topic",
            description = "Renames a system topic while retaining its original slug."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "System topic update payload",
            content = @Content(
                    examples = @ExampleObject(
                            name = "UpdateSystemTopicRequestExample",
                            value = """
                                    {
                                      "name": "Unit 3: Air Travel and Customs"
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "System topic updated successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UpdateSystemTopicSuccessResponse",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 12,
                                        "name": "Unit 3: Air Travel and Customs",
                                        "slug": "unit-3-travelling-and-airports",
                                        "order": 3,
                                        "cardCount": 10,
                                        "learnedCardCount": 0
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<TopicResponse> updateSystemTopic(
            @Parameter(description = "Identifier of the topic", example = "12", required = true)
            @PathVariable Long topicId,
            @Valid @RequestBody UpdateSystemTopicRequest request) {
        return ApiResponse.<TopicResponse>builder()
                .success(true)
                .data(adminTopicService.updateSystemTopic(topicId, request))
                .build();
    }

    @DeleteMapping("/{topicId}")
    @Operation(
            summary = "Delete a system topic",
            description = "Deletes a system topic, moving its cards to orphaned state if student learning history exists, or permanently deleting them if safe."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "System topic deleted successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "DeleteSystemTopicSuccessResponse",
                            value = """
                                    {
                                      "success": true
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<Void> deleteSystemTopic(
            @Parameter(description = "Identifier of the topic to delete", example = "12", required = true)
            @PathVariable Long topicId) {
        adminTopicService.deleteSystemTopic(topicId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @GetMapping("/{topicId}/cards")
    @Operation(
            summary = "Get cards in a topic",
            description = "Retrieves all cards in the specified topic ordered by sequential order."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Cards retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetCardsByTopicIdSuccessResponse",
                            summary = "Sample cards list response",
                            value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "id": 101,
                                          "order": 1,
                                          "term": "boarding pass",
                                          "pos": "noun",
                                          "translation": "thẻ lên máy bay",
                                          "explanationVi": "Thẻ cho phép hành khách lên máy bay",
                                          "explanationEn": "A pass for boarding an aircraft",
                                          "examplesVi": "Vui lòng xuất trình thẻ lên máy bay tại cổng.",
                                          "examplesEn": "Please show your boarding pass at the gate.",
                                          "imageUrl": "https://cdn.elingo.pro/uploads/cards/boarding_pass.png",
                                          "phonetics": [
                                            {
                                              "text": "/ˈbɔː.dɪŋ ˌpɑːs/",
                                              "audioUrl": "https://cdn.elingo.pro/uploads/cards/boarding_pass_uk.mp3",
                                              "locale": "UK"
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<List<CardResponse>> getCardsByTopicId(
            @Parameter(description = "Identifier of the topic", example = "12", required = true)
            @PathVariable Long topicId) {
        return ApiResponse.<List<CardResponse>>builder()
                .success(true)
                .data(adminCardService.getCardsByTopicId(topicId))
                .build();
    }

    @PatchMapping("/{topicId}/cards/orders")
    @Operation(
            summary = "Reorder system cards in a topic",
            description = "Updates the sequential display order of cards in a system topic."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "List of card IDs in desired display order",
            content = @Content(
                    examples = @ExampleObject(
                            name = "ReorderCardsRequestExample",
                            value = """
                                    {
                                      "orderedIds": [102, 101]
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Cards reordered successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "ReorderCardsSuccessResponse",
                            value = """
                                    {
                                      "success": true
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<Void> reorderSystemCards(
            @Parameter(description = "Identifier of the topic", example = "12", required = true)
            @PathVariable Long topicId,
            @Valid @RequestBody ReorderItemRequest request) {
        adminCardService.reorderSystemCards(topicId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
