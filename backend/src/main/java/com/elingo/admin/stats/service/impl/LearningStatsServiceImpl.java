package com.elingo.admin.stats.service.impl;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.CategoryShare;
import com.elingo.admin.stats.dto.response.LearningStatsResponses;
import com.elingo.admin.stats.service.LearningStatsService;
import com.elingo.admin.stats.util.StatsZeroFiller;
import com.elingo.common.service.RedisService;
import com.elingo.file.util.FileKey;
import com.elingo.gamification.dto.projection.ActivityByDateProjection;
import com.elingo.gamification.service.GamificationStatsQueryService;
import com.elingo.learning.lesson.dto.projection.LessonPremiumCountProjection;
import com.elingo.learning.lesson.dto.projection.LessonStatusCountProjection;
import com.elingo.learning.lesson.dto.projection.TopLessonProjection;
import com.elingo.learning.lesson.service.LessonStatsQueryService;
import com.elingo.vocabulary.dto.projection.OwnerTypeCountProjection;
import com.elingo.vocabulary.dto.projection.PremiumCountProjection;
import com.elingo.vocabulary.dto.projection.StatusCountProjection;
import com.elingo.vocabulary.dto.projection.TopDeckProjection;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.service.VocabularyStatsQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "LEARNING-STATS-SERVICE")
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S1192") // Giữ nguyên message log Redis chuẩn thống nhất giữa các hàm stats
public class LearningStatsServiceImpl implements LearningStatsService {

    private final GamificationStatsQueryService gamificationStatsQueryService;
    private final VocabularyStatsQueryService vocabularyStatsQueryService;
    private final LessonStatsQueryService lessonStatsQueryService;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Value("${r2.public-url:}")
    private String publicUrl;

    private static final long CACHE_TTL_ACTIVITY_MINUTES = 3L;
    private static final long CACHE_TTL_DECKS_MINUTES = 5L;
    private static final long CACHE_TTL_LESSONS_MINUTES = 5L;

    @Override
    @SuppressWarnings("java:S3776")
    public LearningStatsResponses.LearningActivityResponse getLearningActivity(StatsDateRangeRequest request) {
        StatsDateRangeRequest safeRequest = sanitizeRequest(request);
        String cacheKey = String.format("admin:stats:learning:activity:from=%s&to=%s&granularity=%s",
                safeRequest.from(), safeRequest.to(), safeRequest.granularity());

        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, LearningStatsResponses.LearningActivityResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize JSON from Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }

        LocalDateTime fromTime = safeRequest.from().atStartOfDay();
        LocalDateTime toTime = safeRequest.to().plusDays(1).atStartOfDay();

        List<ActivityByDateProjection> rawList =
                gamificationStatsQueryService.countActivityBySourceAndDate(fromTime, toTime);

        Map<String, StatsZeroFiller.RawLearningActivity> rawMap = new HashMap<>();
        long totalFlashcard = 0L;
        long totalDictation = 0L;
        long totalShadowing = 0L;
        long totalBattle = 0L;
        long totalCheckIn = 0L;
        long grandTotal = 0L;

        for (ActivityByDateProjection p : rawList) {
            if (p.getDateKey() != null) {
                long fc = p.getFlashcardCount() != null ? p.getFlashcardCount() : 0L;
                long dc = p.getDictationCount() != null ? p.getDictationCount() : 0L;
                long sw = p.getShadowingCount() != null ? p.getShadowingCount() : 0L;
                long bt = p.getBattleCount() != null ? p.getBattleCount() : 0L;
                long ci = p.getCheckInCount() != null ? p.getCheckInCount() : 0L;
                long tot = p.getTotalCount() != null ? p.getTotalCount() : (fc + dc + sw + bt + ci);

                totalFlashcard += fc;
                totalDictation += dc;
                totalShadowing += sw;
                totalBattle += bt;
                totalCheckIn += ci;
                grandTotal += tot;

                rawMap.put(p.getDateKey(), new StatsZeroFiller.RawLearningActivity(fc, dc, sw, bt, ci, tot));
            }
        }

        List<LearningStatsResponses.LearningActivityPoint> points =
                StatsZeroFiller.fillLearningActivitySeries(safeRequest.from(), safeRequest.to(), rawMap);

        LearningStatsResponses.ActivitySourceTotals totals = new LearningStatsResponses.ActivitySourceTotals(
                totalFlashcard,
                totalDictation,
                totalShadowing,
                totalBattle,
                totalCheckIn,
                grandTotal
        );

        LearningStatsResponses.LearningActivityResponse response =
                new LearningStatsResponses.LearningActivityResponse(totals, points, LocalDateTime.now());

        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL_ACTIVITY_MINUTES);
        } catch (JacksonException e) {
            log.error("Failed to serialize JSON to Redis for key {}: {}", cacheKey, e.getMessage());
        }

        return response;
    }

    @Override
    @SuppressWarnings("java:S3776")
    public LearningStatsResponses.DeckStatsResponse getDeckStats(int limit) {
        int safeLimit = Math.max(1, limit);
        String cacheKey = String.format("admin:stats:learning:decks:limit=%d", safeLimit);

        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, LearningStatsResponses.DeckStatsResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize JSON from Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }

        List<StatusCountProjection> statusRaw = vocabularyStatsQueryService.countDecksByStatus();
        long totalDecks = statusRaw.stream()
                .mapToLong(p -> p.getCountVal() != null ? p.getCountVal() : 0L)
                .sum();

        List<CategoryShare<String>> byStatus = statusRaw.stream().map(p -> {
            long count = p.getCountVal() != null ? p.getCountVal() : 0L;
            double pct = totalDecks > 0 ? roundDouble(((double) count / totalDecks) * 100.0) : 0.0;
            return new CategoryShare<>(p.getStatus().name(), count, pct);
        }).toList();

        List<OwnerTypeCountProjection> ownerRaw = vocabularyStatsQueryService.countDecksByOwnerType();
        List<CategoryShare<String>> byOwnerType = ownerRaw.stream().map(p -> {
            long count = p.getCountVal() != null ? p.getCountVal() : 0L;
            double pct = totalDecks > 0 ? roundDouble(((double) count / totalDecks) * 100.0) : 0.0;
            return new CategoryShare<>(p.getOwnerType().name(), count, pct);
        }).toList();

        List<PremiumCountProjection> premiumRaw = vocabularyStatsQueryService.countDecksByPremium();
        List<CategoryShare<String>> byPremium = premiumRaw.stream().map(p -> {
            long count = p.getCountVal() != null ? p.getCountVal() : 0L;
            double pct = totalDecks > 0 ? roundDouble(((double) count / totalDecks) * 100.0) : 0.0;
            String category = Boolean.TRUE.equals(p.getIsPremium()) ? "PREMIUM" : "FREE";
            return new CategoryShare<>(category, count, pct);
        }).toList();

        List<TopDeckProjection> systemDecksRaw =
                vocabularyStatsQueryService.getTopDecksByOwnerType(OwnerType.SYSTEM, safeLimit);
        List<LearningStatsResponses.TopDeckItem> topSystemDecks = systemDecksRaw.stream().map(p ->
                new LearningStatsResponses.TopDeckItem(
                        p.getDeckId(),
                        p.getTitle(),
                        resolveMediaUrl(p.getCoverImageUrl()),
                        p.getStatus(),
                        p.getOwnerType(),
                        p.getIsPremium(),
                        p.getLearnerCount() != null ? p.getLearnerCount() : 0L
                )
        ).toList();

        List<TopDeckProjection> userDecksRaw =
                vocabularyStatsQueryService.getTopDecksByOwnerType(OwnerType.USER, safeLimit);
        List<LearningStatsResponses.TopDeckItem> topUserDecks = userDecksRaw.stream().map(p ->
                new LearningStatsResponses.TopDeckItem(
                        p.getDeckId(),
                        p.getTitle(),
                        resolveMediaUrl(p.getCoverImageUrl()),
                        p.getStatus(),
                        p.getOwnerType(),
                        p.getIsPremium(),
                        p.getLearnerCount() != null ? p.getLearnerCount() : 0L
                )
        ).toList();

        LearningStatsResponses.DeckStatsResponse response = new LearningStatsResponses.DeckStatsResponse(
                totalDecks,
                byStatus,
                byOwnerType,
                byPremium,
                topSystemDecks,
                topUserDecks,
                LocalDateTime.now()
        );

        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL_DECKS_MINUTES);
        } catch (JacksonException e) {
            log.error("Failed to serialize JSON to Redis for key {}: {}", cacheKey, e.getMessage());
        }

        return response;
    }

    @Override
    @SuppressWarnings("java:S3776")
    public LearningStatsResponses.LessonStatsResponse getLessonStats(int limit) {
        int safeLimit = Math.max(1, limit);
        String cacheKey = String.format("admin:stats:learning:lessons:limit=%d", safeLimit);

        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, LearningStatsResponses.LessonStatsResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize JSON from Redis for key {}: {}", cacheKey, e.getMessage());
            }
        }

        List<LessonStatusCountProjection> statusRaw = lessonStatsQueryService.countLessonsByStatus();
        long totalLessons = statusRaw.stream()
                .mapToLong(p -> p.getCountVal() != null ? p.getCountVal() : 0L)
                .sum();

        List<CategoryShare<String>> byStatus = statusRaw.stream().map(p -> {
            long count = p.getCountVal() != null ? p.getCountVal() : 0L;
            double pct = totalLessons > 0 ? roundDouble(((double) count / totalLessons) * 100.0) : 0.0;
            return new CategoryShare<>(p.getStatus().name(), count, pct);
        }).toList();

        List<LessonPremiumCountProjection> premiumRaw = lessonStatsQueryService.countLessonsByPremium();
        List<CategoryShare<String>> byPremium = premiumRaw.stream().map(p -> {
            long count = p.getCountVal() != null ? p.getCountVal() : 0L;
            double pct = totalLessons > 0 ? roundDouble(((double) count / totalLessons) * 100.0) : 0.0;
            String category = Boolean.TRUE.equals(p.getIsPremium()) ? "PREMIUM" : "FREE";
            return new CategoryShare<>(category, count, pct);
        }).toList();

        List<TopLessonProjection> topRaw = lessonStatsQueryService.getTopLessonsByCompletion(safeLimit);
        List<LearningStatsResponses.TopLessonItem> topLessons = topRaw.stream().map(p ->
                new LearningStatsResponses.TopLessonItem(
                        p.getLessonId(),
                        p.getTitle(),
                        resolveMediaUrl(p.getThumbnailUrl()),
                        p.getStatus(),
                        p.getIsPremium(),
                        p.getLearnerCount() != null ? p.getLearnerCount() : 0L
                )
        ).toList();

        LearningStatsResponses.LessonStatsResponse response = new LearningStatsResponses.LessonStatsResponse(
                totalLessons,
                byStatus,
                byPremium,
                topLessons,
                LocalDateTime.now()
        );

        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL_LESSONS_MINUTES);
        } catch (JacksonException e) {
            log.error("Failed to serialize JSON to Redis for key {}: {}", cacheKey, e.getMessage());
        }

        return response;
    }

    private StatsDateRangeRequest sanitizeRequest(StatsDateRangeRequest request) {
        if (request == null) {
            return new StatsDateRangeRequest(null, null, null);
        }
        return request;
    }

    private double roundDouble(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String resolveMediaUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (publicUrl == null || publicUrl.isBlank()) {
            return key;
        }
        return FileKey.toPublicUrl(publicUrl, key);
    }
}
