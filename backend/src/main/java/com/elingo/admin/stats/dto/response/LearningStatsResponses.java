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
            @Schema(description = "Total flashcard review events accumulated", example = "450")
            Long totalFlashcard,

            @Schema(description = "Total dictation completion events accumulated", example = "200")
            Long totalDictation,

            @Schema(description = "Total shadowing completion events accumulated", example = "150")
            Long totalShadowing,

            @Schema(description = "Total battle match events accumulated", example = "300")
            Long totalBattle,

            @Schema(description = "Total daily check-in events accumulated", example = "800")
            Long totalCheckIn,

            @Schema(description = "Grand total of all learning events accumulated", example = "1900")
            Long grandTotal
    ) {}

    @Schema(description = "Learning activity time-series response for stacked bar visualization")
    public record LearningActivityResponse(
            @Schema(description = "Cumulative totals for each learning activity source")
            ActivitySourceTotals totals,

            @Schema(description = "Continuous daily activity time-series list with zero-filled gaps")
            List<LearningActivityPoint> points,

            @Schema(description = "Timestamp when this statistic report was generated", example = "2026-11-20T21:00:00")
            LocalDateTime generatedAt
    ) {}

    @Schema(description = "Top deck ranked by number of unique learners")
    public record TopDeckItem(
            @Schema(description = "Unique identifier of the deck", example = "1")
            Long deckId,

            @Schema(description = "Title of the deck", example = "Oxford 3000 Essential Words")
            String title,

            @Schema(description = "Cover image URL of the deck", example = "https://cdn.elingo.com/decks/cover1.jpg")
            String coverImageUrl,

            @Schema(description = "Publication status of the deck", example = "PUBLISHED")
            String status,

            @Schema(description = "Owner type of the deck (SYSTEM or USER)", example = "SYSTEM")
            String ownerType,

            @Schema(description = "Indicates whether the deck requires premium subscription", example = "false")
            Boolean isPremium,

            @Schema(description = "Total number of unique users learning this deck", example = "1250")
            Long learnerCount
    ) {}

    @Schema(description = "Deck distribution statistics and top learned decks separated by system and user ownership")
    public record DeckStatsResponse(
            @Schema(description = "Total registered decks in the system", example = "150")
            Long totalDecks,

            @Schema(description = "Distribution of decks grouped by publication status")
            List<CategoryShare<String>> byStatus,

            @Schema(description = "Distribution of decks grouped by owner type (SYSTEM vs USER)")
            List<CategoryShare<String>> byOwnerType,

            @Schema(description = "Distribution of decks grouped by premium tier (PREMIUM vs FREE)")
            List<CategoryShare<String>> byPremium,

            @Schema(description = "Top system decks ranked by unique learner count")
            List<TopDeckItem> topSystemDecks,

            @Schema(description = "Top community user decks ranked by unique learner count")
            List<TopDeckItem> topUserDecks,

            @Schema(description = "Timestamp when this statistic report was generated", example = "2026-11-20T21:00:00")
            LocalDateTime generatedAt
    ) {}

    @Schema(description = "Top lesson ranked by number of unique learners")
    public record TopLessonItem(
            @Schema(description = "Unique identifier of the lesson", example = "10")
            Long lessonId,

            @Schema(description = "Title of the lesson", example = "Daily English Conversation in Coffee Shop")
            String title,

            @Schema(description = "Thumbnail image URL of the lesson video", example = "https://cdn.elingo.com/lessons/thumb10.jpg")
            String thumbnailUrl,

            @Schema(description = "Publication status of the lesson", example = "PUBLISHED")
            String status,

            @Schema(description = "Indicates whether the lesson requires premium subscription", example = "true")
            Boolean isPremium,

            @Schema(description = "Total number of unique learners enrolled or interacted with this lesson", example = "850")
            Long learnerCount
    ) {}

    @Schema(description = "Lesson distribution statistics and top learned lessons")
    public record LessonStatsResponse(
            @Schema(description = "Total registered lessons in the system", example = "80")
            Long totalLessons,

            @Schema(description = "Distribution of lessons grouped by publication status")
            List<CategoryShare<String>> byStatus,

            @Schema(description = "Distribution of lessons grouped by premium tier (PREMIUM vs FREE)")
            List<CategoryShare<String>> byPremium,

            @Schema(description = "Top lessons ranked by learner count")
            List<TopLessonItem> topLessons,

            @Schema(description = "Timestamp when this statistic report was generated", example = "2026-11-20T21:00:00")
            LocalDateTime generatedAt
    ) {}
}
