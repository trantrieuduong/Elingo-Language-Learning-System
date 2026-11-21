package com.elingo.admin.stats.controller;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.LearningStatsResponses;
import com.elingo.admin.stats.service.LearningStatsService;
import com.elingo.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
@RequestMapping("/admin/stats/learning")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Learning Stats",
        description = "Administrative analytics endpoints for learning activities, deck distributions, and lesson performance")
public class LearningStatsController {

    private final LearningStatsService learningStatsService;

    @GetMapping("/activity")
    @Operation(
            summary = "Get daily learning activity xp events by source",
            description = "Retrieves continuous daily learning activity events categorized by source (flashcard, dictation, shadowing, battle, check-in) for stacked bar visualization. Missing dates are zero-filled."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Learning activities retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid date range parameters"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or expired authentication token"
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
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or expired authentication token"
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
            summary = "Get lesson distribution and top completed lessons",
            description = "Retrieves lesson counts grouped by status, overall average dictation and shadowing scores, and top lessons ranked by learner completion counts."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or expired authentication token"
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
