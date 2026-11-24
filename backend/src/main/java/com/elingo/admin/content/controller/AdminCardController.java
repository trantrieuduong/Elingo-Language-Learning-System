package com.elingo.admin.content.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.vocabulary.dto.request.CreateSystemCardRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemCardRequest;
import com.elingo.admin.content.service.AdminCardService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
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

@RestController
@RequestMapping(AdminApiPaths.CARDS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Cards",
        description = "Management endpoints for system flashcards in the admin console"
)
public class AdminCardController {

    private final AdminCardService adminCardService;

    @GetMapping("/{cardId}")
    @Operation(
            summary = "Get card details by ID",
            description = "Retrieves full details of a flashcard including phonetics, translations, and media URLs."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Card details retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetCardByIdSuccessResponse",
                            summary = "Sample card detail response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 101,
                                        "order": 1,
                                        "term": "perseverance",
                                        "pos": "noun",
                                        "translation": "sự kiên trì, bền chí",
                                        "explanationVi": "Tính kiên trì bền bỉ vượt qua khó khăn",
                                        "explanationEn": "Persistence in doing something despite difficulty",
                                        "examplesVi": "Thành công đòi hỏi sự kiên trì liên tục.",
                                        "examplesEn": "Success requires sustained perseverance.",
                                        "imageUrl": "https://cdn.elingo.pro/uploads/cards/perseverance.jpg",
                                        "phonetics": [
                                          {
                                            "text": "/ˌpɜː.sɪˈvɪə.rəns/",
                                            "audioUrl": "https://cdn.elingo.pro/uploads/cards/perseverance_uk.mp3",
                                            "locale": "UK"
                                          },
                                          {
                                            "text": "/ˌpɝː.səˈvɪr.əns/",
                                            "audioUrl": "https://cdn.elingo.pro/uploads/cards/perseverance_us.mp3",
                                            "locale": "US"
                                          }
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<CardResponse> getCardById(
            @Parameter(description = "Identifier of the card", example = "101", required = true)
            @PathVariable Long cardId) {
        return ApiResponse.<CardResponse>builder()
                .success(true)
                .data(adminCardService.getCardById(cardId))
                .build();
    }

    @PostMapping
    @Operation(
            summary = "Create a new system card",
            description = "Creates a new flashcard with verified media files in a system topic."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "System card creation payload",
            content = @Content(
                    examples = @ExampleObject(
                            name = "CreateSystemCardRequestExample",
                            value = """
                                    {
                                      "topicId": 12,
                                      "term": "perseverance",
                                      "pos": "noun",
                                      "translation": "sự kiên trì, bền chí",
                                      "explanationVi": "Tính kiên trì bền bỉ vượt qua khó khăn",
                                      "explanationEn": "Persistence in doing something despite difficulty",
                                      "examplesVi": "Thành công đòi hỏi sự kiên trì liên tục.",
                                      "examplesEn": "Success requires sustained perseverance.",
                                      "imageUrl": "verified/cards/perseverance.jpg",
                                      "phonetics": [
                                        {
                                          "text": "/ˌpɜː.sɪˈvɪə.rəns/",
                                          "audioUrl": "verified/cards/perseverance_uk.mp3",
                                          "locale": "UK"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "System card created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "CreateSystemCardSuccessResponse",
                            summary = "Sample created card response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 101,
                                        "order": 1,
                                        "term": "perseverance",
                                        "pos": "noun",
                                        "translation": "sự kiên trì, bền chí",
                                        "explanationVi": "Tính kiên trì bền bỉ vượt qua khó khăn",
                                        "explanationEn": "Persistence in doing something despite difficulty",
                                        "examplesVi": "Thành công đòi hỏi sự kiên trì liên tục.",
                                        "examplesEn": "Success requires sustained perseverance.",
                                        "imageUrl": "https://cdn.elingo.pro/uploads/cards/perseverance.jpg",
                                        "phonetics": [
                                          {
                                            "text": "/ˌpɜː.sɪˈvɪə.rəns/",
                                            "audioUrl": "https://cdn.elingo.pro/uploads/cards/perseverance_uk.mp3",
                                            "locale": "UK"
                                          }
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<CardResponse> createSystemCard(
            @Valid @RequestBody CreateSystemCardRequest request) {
        return ApiResponse.<CardResponse>builder()
                .success(true)
                .data(adminCardService.createSystemCard(request))
                .build();
    }

    @PutMapping("/{cardId}")
    @Operation(
            summary = "Update an existing system card",
            description = "Updates term, definition, examples, phonetics, and media files for an existing card."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "System card update payload",
            content = @Content(
                    examples = @ExampleObject(
                            name = "UpdateSystemCardRequestExample",
                            value = """
                                    {
                                      "term": "perseverance",
                                      "pos": "noun",
                                      "translation": "sự kiên trì, bền chí, quyết tâm",
                                      "explanationVi": "Tính kiên trì bền bỉ vượt qua trở ngại",
                                      "explanationEn": "Continued effort to do or achieve something despite difficulties",
                                      "examplesVi": "Với sự kiên trì, cô ấy đã vượt qua mọi trở ngại.",
                                      "examplesEn": "With perseverance, she overcame all obstacles.",
                                      "imageUrl": "uploads/cards/perseverance.jpg",
                                      "phonetics": [
                                        {
                                          "text": "/ˌpɜː.sɪˈvɪə.rəns/",
                                          "audioUrl": "uploads/cards/perseverance_uk.mp3",
                                          "locale": "UK"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "System card updated successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UpdateSystemCardSuccessResponse",
                            summary = "Sample updated card response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 101,
                                        "order": 1,
                                        "term": "perseverance",
                                        "pos": "noun",
                                        "translation": "sự kiên trì, bền chí, quyết tâm",
                                        "explanationVi": "Tính kiên trì bền bỉ vượt qua trở ngại",
                                        "explanationEn": "Continued effort to do or achieve something despite difficulties",
                                        "examplesVi": "Với sự kiên trì, cô ấy đã vượt qua mọi trở ngại.",
                                        "examplesEn": "With perseverance, she overcame all obstacles.",
                                        "imageUrl": "https://cdn.elingo.pro/uploads/cards/perseverance.jpg",
                                        "phonetics": [
                                          {
                                            "text": "/ˌpɜː.sɪˈvɪə.rəns/",
                                            "audioUrl": "https://cdn.elingo.pro/uploads/cards/perseverance_uk.mp3",
                                            "locale": "UK"
                                          }
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<CardResponse> updateSystemCard(
            @Parameter(description = "Identifier of the card to update", example = "101", required = true)
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateSystemCardRequest request) {
        return ApiResponse.<CardResponse>builder()
                .success(true)
                .data(adminCardService.updateSystemCard(cardId, request))
                .build();
    }

    @DeleteMapping("/{cardId}")
    @Operation(
            summary = "Delete a system card",
            description = "Deletes a system card. If learners have active SRS review history on this card, it transitions to orphaned state; otherwise, it is permanently deleted along with its media files."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "System card deleted successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "DeleteSystemCardSuccessResponse",
                            value = """
                                    {
                                      "success": true
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<Void> deleteSystemCard(
            @Parameter(description = "Identifier of the card to delete", example = "101", required = true)
            @PathVariable Long cardId) {
        adminCardService.deleteSystemCard(cardId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
