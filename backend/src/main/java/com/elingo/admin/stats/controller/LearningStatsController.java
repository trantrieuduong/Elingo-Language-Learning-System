package com.elingo.admin.stats.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.LearningStatsResponses;
import com.elingo.admin.stats.service.LearningStatsService;
import com.elingo.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminApiPaths.STATS_LEARNING)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Stats Learning",
        description = "Administrative analytics endpoints for learning activities, deck distributions, and lesson performance")
public class LearningStatsController {

    private final LearningStatsService learningStatsService;

    @GetMapping("/activity")
    @Operation(
            summary = "Get daily learning activity xp events by source",
            description = "Retrieves continuous daily learning activity events categorized by source (flashcard, dictation, shadowing, battle, check-in) for stacked bar visualization. Missing dates are zero-filled automatically."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Learning activities retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "LearningActivitySuccessResponse",
                            summary = "Sample learning activity daily response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "totals": {
                                          "totalFlashcard": 450,
                                          "totalDictation": 200,
                                          "totalShadowing": 150,
                                          "totalBattle": 300,
                                          "totalCheckIn": 800,
                                          "grandTotal": 1900
                                        },
                                        "points": [
                                          {
                                            "date": "2026-11-01",
                                            "flashcard": 45,
                                            "dictation": 20,
                                            "shadowing": 15,
                                            "battle": 30,
                                            "checkIn": 80,
                                            "total": 190
                                          },
                                          {
                                            "date": "2026-11-02",
                                            "flashcard": 0,
                                            "dictation": 0,
                                            "shadowing": 0,
                                            "battle": 0,
                                            "checkIn": 0,
                                            "total": 0
                                          }
                                        ],
                                        "generatedAt": "2026-11-20T21:00:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid date range parameters"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<LearningStatsResponses.LearningActivityResponse> getLearningActivity(
            @Valid @ParameterObject StatsDateRangeRequest request
    ) {
        return ApiResponse.<LearningStatsResponses.LearningActivityResponse>builder()
                .success(true)
                .data(learningStatsService.getLearningActivity(request))
                .build();
    }

    @GetMapping("/decks")
    @Operation(
            summary = "Get deck distribution and top learned decks",
            description = "Calculates deck counts and percentages grouped by status, owner type, and premium status, alongside top system decks and top user decks ranked by unique learners."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Deck statistics retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "DeckStatsSuccessResponse",
                            summary = "Sample deck distribution response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "totalDecks": 150,
                                        "byStatus": [
                                          { "category": "PUBLISHED", "count": 120, "percentage": 80.0 },
                                          { "category": "DRAFT", "count": 25, "percentage": 16.67 },
                                          { "category": "ARCHIVED", "count": 5, "percentage": 3.33 }
                                        ],
                                        "byOwnerType": [
                                          { "category": "SYSTEM", "count": 50, "percentage": 33.33 },
                                          { "category": "USER", "count": 100, "percentage": 66.67 }
                                        ],
                                        "byPremium": [
                                          { "category": "FREE", "count": 110, "percentage": 73.33 },
                                          { "category": "PREMIUM", "count": 40, "percentage": 26.67 }
                                        ],
                                        "topSystemDecks": [
                                          {
                                            "deckId": 1,
                                            "title": "Oxford 3000 Essential Words",
                                            "coverImageUrl": "https://cdn.elingo.com/decks/cover1.jpg",
                                            "status": "PUBLISHED",
                                            "ownerType": "SYSTEM",
                                            "isPremium": false,
                                            "learnerCount": 1250
                                          }
                                        ],
                                        "topUserDecks": [
                                          {
                                            "deckId": 45,
                                            "title": "Medical English Terminology",
                                            "coverImageUrl": null,
                                            "status": "PUBLISHED",
                                            "ownerType": "USER",
                                            "isPremium": false,
                                            "learnerCount": 320
                                          }
                                        ],
                                        "generatedAt": "2026-11-20T21:00:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<LearningStatsResponses.DeckStatsResponse> getDeckStats(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.<LearningStatsResponses.DeckStatsResponse>builder()
                .success(true)
                .data(learningStatsService.getDeckStats(limit))
                .build();
    }

    @GetMapping("/lessons")
    @Operation(
            summary = "Get lesson distribution and top learned lessons",
            description = "Retrieves lesson counts grouped by status and premium tier, alongside top lessons ranked by learner completion counts."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson statistics retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "LessonStatsSuccessResponse",
                            summary = "Sample lesson distribution response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "totalLessons": 80,
                                        "byStatus": [
                                          { "category": "PUBLISHED", "count": 70, "percentage": 87.5 },
                                          { "category": "DRAFT", "count": 10, "percentage": 12.5 }
                                        ],
                                        "byPremium": [
                                          { "category": "FREE", "count": 55, "percentage": 68.75 },
                                          { "category": "PREMIUM", "count": 25, "percentage": 31.25 }
                                        ],
                                        "topLessons": [
                                          {
                                            "lessonId": 10,
                                            "title": "Daily English Conversation in Coffee Shop",
                                            "thumbnailUrl": "https://cdn.elingo.com/lessons/thumb10.jpg",
                                            "status": "PUBLISHED",
                                            "isPremium": false,
                                            "learnerCount": 850
                                          }
                                        ],
                                        "generatedAt": "2026-11-20T21:00:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<LearningStatsResponses.LessonStatsResponse> getLessonStats(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.<LearningStatsResponses.LessonStatsResponse>builder()
                .success(true)
                .data(learningStatsService.getLessonStats(limit))
                .build();
    }
}
