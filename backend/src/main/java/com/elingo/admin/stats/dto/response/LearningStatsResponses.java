package com.elingo.admin.stats.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class LearningStatsResponses {
    private LearningStatsResponses() {}

    @Schema(description = "Data point representing learning events count by source on a specific date")
    public record LearningActivityPoint(
            @Schema(description = "Date of activity (YYYY-MM-DD)", example = "2026-11-20")
            LocalDate date,
            @Schema(description = "Flashcard review events count", example = "45")
            Long flashcard,
            @Schema(description = "Dictation completion events count", example = "20")
            Long dictation,
            @Schema(description = "Shadowing completion events count", example = "15")
            Long shadowing,
            @Schema(description = "Battle match events count", example = "30")
            Long battle,
            @Schema(description = "Daily check-in events count", example = "80")
            Long checkIn,
            @Schema(description = "Total events on this date", example = "190")
            Long total
    ) {}

    @Schema(description = "Aggregated count for each activity source over the query range")
    public record ActivitySourceTotals(
            Long totalFlashcard,
            Long totalDictation,
            Long totalShadowing,
            Long totalBattle,
            Long totalCheckIn,
            Long grandTotal
    ) {}

    @Schema(description = "Learning activity time-series response for stacked bar visualization")
    public record LearningActivityResponse(
            ActivitySourceTotals totals,
            List<LearningActivityPoint> points,
            LocalDateTime generatedAt
    ) {}

    @Schema(description = "Top deck ranked by number of unique learners")
    public record TopDeckItem(
            Long deckId,
            String title,
            String coverImageUrl,
            String status,
            String ownerType,
            Boolean isPremium,
            Long learnerCount
    ) {}

    @Schema(description = "Deck distribution statistics and top learned decks separated by system and user ownership")
    public record DeckStatsResponse(
            Long totalDecks,
            List<CategoryShare<String>> byStatus,
            List<CategoryShare<String>> byOwnerType,
            List<CategoryShare<String>> byPremium,
            List<TopDeckItem> topSystemDecks,
            List<TopDeckItem> topUserDecks,
            LocalDateTime generatedAt
    ) {}

    @Schema(description = "Top lesson ranked by number of learners")
    public record TopLessonItem(
            Long lessonId,
            String title,
            String thumbnailUrl,
            String status,
            Boolean isPremium,
            Long learnerCount
    ) {}

    @Schema(description = "Lesson distribution statistics and top completed lessons with average scores")
    public record LessonStatsResponse(
            Long totalLessons,
            List<CategoryShare<String>> byStatus,
            List<CategoryShare<String>> byPremium,
            List<TopLessonItem> topLessons,
            LocalDateTime generatedAt
    ) {}
}
