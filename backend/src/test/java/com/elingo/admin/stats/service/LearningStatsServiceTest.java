package com.elingo.admin.stats.service;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.LearningStatsResponses;
import com.elingo.admin.stats.service.impl.LearningStatsServiceImpl;
import com.elingo.common.enums.StatsGranularity;
import com.elingo.common.service.RedisService;
import com.elingo.gamification.dto.projection.ActivityByDateProjection;
import com.elingo.gamification.service.GamificationStatsQueryService;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.learning.lesson.dto.projection.LessonPremiumCountProjection;
import com.elingo.learning.lesson.dto.projection.LessonStatusCountProjection;
import com.elingo.learning.lesson.dto.projection.TopLessonProjection;
import com.elingo.learning.lesson.service.LessonStatsQueryService;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.dto.projection.OwnerTypeCountProjection;
import com.elingo.vocabulary.dto.projection.PremiumCountProjection;
import com.elingo.vocabulary.dto.projection.StatusCountProjection;
import com.elingo.vocabulary.dto.projection.TopDeckProjection;
import com.elingo.vocabulary.service.VocabularyStatsQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LearningStatsServiceTest {

    @Mock
    private GamificationStatsQueryService gamificationStatsQueryService;

    @Mock
    private VocabularyStatsQueryService vocabularyStatsQueryService;

    @Mock
    private LessonStatsQueryService lessonStatsQueryService;

    @Mock
    private RedisService redisService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LearningStatsServiceImpl learningStatsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(learningStatsService, "publicUrl", "https://cdn.elingo.com");
    }

    private final LocalDate fromDate = LocalDate.of(2026, 11, 1);
    private final LocalDate toDate = LocalDate.of(2026, 11, 3);
    private final StatsDateRangeRequest defaultRequest = StatsDateRangeRequest.of(fromDate, toDate, StatsGranularity.DAY);

    @Nested
    @DisplayName("getLearningActivity")
    class GetLearningActivityTests {

        @Test
        @DisplayName("Should return cached response when present in Redis")
        void getLearningActivity_CacheHit_ReturnsCached() throws Exception {
            String cacheKey = "admin:stats:learning:activity:from=2026-11-01&to=2026-11-03&granularity=DAY";
            String jsonContent = "{\"totals\":{\"grandTotal\":100}}";
            LearningStatsResponses.LearningActivityResponse expected = new LearningStatsResponses.LearningActivityResponse(
                    new LearningStatsResponses.ActivitySourceTotals(10L, 20L, 30L, 20L, 20L, 100L),
                    List.of(),
                    LocalDateTime.now()
            );

            when(redisService.get(cacheKey)).thenReturn(jsonContent);
            when(objectMapper.readValue(jsonContent, LearningStatsResponses.LearningActivityResponse.class)).thenReturn(expected);

            LearningStatsResponses.LearningActivityResponse actual = learningStatsService.getLearningActivity(defaultRequest);

            assertThat(actual).isEqualTo(expected);
            verifyNoInteractions(gamificationStatsQueryService);
        }

        @Test
        @DisplayName("Should fetch from GamificationStatsQueryService, apply zero-filling, and save to cache on cache miss")
        void getLearningActivity_CacheMiss_FetchesAndCaches() throws Exception {
            String cacheKey = "admin:stats:learning:activity:from=2026-11-01&to=2026-11-03&granularity=DAY";
            when(redisService.get(cacheKey)).thenReturn(null);

            ActivityByDateProjection proj = mock(ActivityByDateProjection.class);
            when(proj.getDateKey()).thenReturn("2026-11-01");
            when(proj.getFlashcardCount()).thenReturn(5L);
            when(proj.getDictationCount()).thenReturn(10L);
            when(proj.getShadowingCount()).thenReturn(15L);
            when(proj.getBattleCount()).thenReturn(20L);
            when(proj.getCheckInCount()).thenReturn(25L);
            when(proj.getTotalCount()).thenReturn(75L);

            when(gamificationStatsQueryService.countActivityBySourceAndDate(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(proj));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"cached\":true}");

            LearningStatsResponses.LearningActivityResponse actual = learningStatsService.getLearningActivity(defaultRequest);

            assertThat(actual).isNotNull();
            assertThat(actual.totals().totalFlashcard()).isEqualTo(5L);
            assertThat(actual.totals().totalDictation()).isEqualTo(10L);
            assertThat(actual.totals().totalShadowing()).isEqualTo(15L);
            assertThat(actual.totals().totalBattle()).isEqualTo(20L);
            assertThat(actual.totals().totalCheckIn()).isEqualTo(25L);
            assertThat(actual.totals().grandTotal()).isEqualTo(75L);
            assertThat(actual.points()).hasSize(3); // 2026-11-01, 2026-11-02, 2026-11-03

            // Date 2026-11-02 should be zero-filled
            LearningStatsResponses.LearningActivityPoint day2 = actual.points().get(1);
            assertThat(day2.date()).isEqualTo(LocalDate.of(2026, 11, 2));
            assertThat(day2.total()).isEqualTo(0L);

            verify(redisService).save(eq(cacheKey), eq("{\"cached\":true}"), eq(3L));
        }

        @Test
        @DisplayName("Should fall back to database query when Redis cache read throws JacksonException")
        void getLearningActivity_CacheReadError_FallsBackToDb() throws Exception {
            String cacheKey = "admin:stats:learning:activity:from=2026-11-01&to=2026-11-03&granularity=DAY";
            when(redisService.get(cacheKey)).thenReturn("invalid-json");
            when(objectMapper.readValue(eq("invalid-json"), eq(LearningStatsResponses.LearningActivityResponse.class)))
                    .thenThrow(mock(JacksonException.class));

            when(gamificationStatsQueryService.countActivityBySourceAndDate(any(), any()))
                    .thenReturn(Collections.emptyList());

            LearningStatsResponses.LearningActivityResponse actual = learningStatsService.getLearningActivity(defaultRequest);

            assertThat(actual).isNotNull();
            assertThat(actual.totals().grandTotal()).isEqualTo(0L);
            verify(gamificationStatsQueryService).countActivityBySourceAndDate(any(), any());
        }

        @Test
        @DisplayName("Should return response normally even when Redis cache serialization fails")
        void getLearningActivity_CacheWriteError_ReturnsResponse() throws Exception {
            when(redisService.get(anyString())).thenReturn(null);
            when(gamificationStatsQueryService.countActivityBySourceAndDate(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(objectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));

            LearningStatsResponses.LearningActivityResponse actual = learningStatsService.getLearningActivity(defaultRequest);

            assertThat(actual).isNotNull();
            assertThat(actual.points()).hasSize(3);
        }

        @Test
        @DisplayName("Should sanitize request when null request is passed")
        void getLearningActivity_NullRequest_Sanitized() {
            when(redisService.get(anyString())).thenReturn(null);
            when(gamificationStatsQueryService.countActivityBySourceAndDate(any(), any()))
                    .thenReturn(Collections.emptyList());

            LearningStatsResponses.LearningActivityResponse actual = learningStatsService.getLearningActivity(null);

            assertThat(actual).isNotNull();
            assertThat(actual.points()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("getDeckStats")
    class GetDeckStatsTests {

        @Test
        @DisplayName("Should return cached response when present in Redis")
        void getDeckStats_CacheHit_ReturnsCached() throws Exception {
            String cacheKey = "admin:stats:learning:decks:limit=10";
            String jsonContent = "{\"totalDecks\":50}";
            LearningStatsResponses.DeckStatsResponse expected = new LearningStatsResponses.DeckStatsResponse(
                    50L, List.of(), List.of(), List.of(), List.of(), List.of(), LocalDateTime.now()
            );

            when(redisService.get(cacheKey)).thenReturn(jsonContent);
            when(objectMapper.readValue(jsonContent, LearningStatsResponses.DeckStatsResponse.class)).thenReturn(expected);

            LearningStatsResponses.DeckStatsResponse actual = learningStatsService.getDeckStats(10);

            assertThat(actual).isEqualTo(expected);
            verifyNoInteractions(vocabularyStatsQueryService);
        }

        @Test
        @DisplayName("Should fetch distributions and top decks, then cache on cache miss")
        void getDeckStats_CacheMiss_FetchesAndCaches() throws Exception {
            String cacheKey = "admin:stats:learning:decks:limit=10";
            when(redisService.get(cacheKey)).thenReturn(null);

            StatusCountProjection statusProj = mock(StatusCountProjection.class);
            when(statusProj.getStatus()).thenReturn(DeckStatus.PUBLISHED);
            when(statusProj.getCountVal()).thenReturn(80L);
            when(vocabularyStatsQueryService.countDecksByStatus()).thenReturn(List.of(statusProj));

            OwnerTypeCountProjection ownerProj = mock(OwnerTypeCountProjection.class);
            when(ownerProj.getOwnerType()).thenReturn(OwnerType.SYSTEM);
            when(ownerProj.getCountVal()).thenReturn(80L);
            when(vocabularyStatsQueryService.countDecksByOwnerType()).thenReturn(List.of(ownerProj));

            PremiumCountProjection premiumProj = mock(PremiumCountProjection.class);
            when(premiumProj.getIsPremium()).thenReturn(true);
            when(premiumProj.getCountVal()).thenReturn(80L);
            when(vocabularyStatsQueryService.countDecksByPremium()).thenReturn(List.of(premiumProj));

            TopDeckProjection topSys = mock(TopDeckProjection.class);
            when(topSys.getDeckId()).thenReturn(1L);
            when(topSys.getTitle()).thenReturn("System Deck");
            when(topSys.getCoverImageUrl()).thenReturn("img.jpg");
            when(topSys.getStatus()).thenReturn("PUBLISHED");
            when(topSys.getOwnerType()).thenReturn("SYSTEM");
            when(topSys.getIsPremium()).thenReturn(true);
            when(topSys.getLearnerCount()).thenReturn(500L);
            when(vocabularyStatsQueryService.getTopDecksByOwnerType(OwnerType.SYSTEM, 10)).thenReturn(List.of(topSys));

            TopDeckProjection topUser = mock(TopDeckProjection.class);
            when(topUser.getDeckId()).thenReturn(2L);
            when(topUser.getTitle()).thenReturn("User Deck");
            when(topUser.getCoverImageUrl()).thenReturn(null);
            when(topUser.getStatus()).thenReturn("PUBLISHED");
            when(topUser.getOwnerType()).thenReturn("USER");
            when(topUser.getIsPremium()).thenReturn(false);
            when(topUser.getLearnerCount()).thenReturn(150L);
            when(vocabularyStatsQueryService.getTopDecksByOwnerType(OwnerType.USER, 10)).thenReturn(List.of(topUser));

            when(objectMapper.writeValueAsString(any())).thenReturn("{\"cached\":true}");

            LearningStatsResponses.DeckStatsResponse actual = learningStatsService.getDeckStats(10);

            assertThat(actual.totalDecks()).isEqualTo(80L);
            assertThat(actual.byStatus()).hasSize(1);
            assertThat(actual.byStatus().get(0).category()).isEqualTo("PUBLISHED");
            assertThat(actual.byStatus().get(0).percentage()).isEqualTo(100.0);
            assertThat(actual.byPremium().get(0).category()).isEqualTo("PREMIUM");
            assertThat(actual.topSystemDecks()).hasSize(1);
            assertThat(actual.topSystemDecks().get(0).title()).isEqualTo("System Deck");
            assertThat(actual.topSystemDecks().get(0).coverImageUrl()).isEqualTo("https://cdn.elingo.com/img.jpg");
            assertThat(actual.topUserDecks()).hasSize(1);
            assertThat(actual.topUserDecks().get(0).title()).isEqualTo("User Deck");
            assertThat(actual.topUserDecks().get(0).coverImageUrl()).isNull();

            verify(redisService).save(eq(cacheKey), eq("{\"cached\":true}"), eq(5L));
        }

        @Test
        @DisplayName("Should handle zero total decks without division by zero")
        void getDeckStats_ZeroTotalDecks_HandlesZeroPercentage() {
            when(redisService.get(anyString())).thenReturn(null);
            when(vocabularyStatsQueryService.countDecksByStatus()).thenReturn(Collections.emptyList());
            when(vocabularyStatsQueryService.countDecksByOwnerType()).thenReturn(Collections.emptyList());
            when(vocabularyStatsQueryService.countDecksByPremium()).thenReturn(Collections.emptyList());
            when(vocabularyStatsQueryService.getTopDecksByOwnerType(any(), anyInt())).thenReturn(Collections.emptyList());

            LearningStatsResponses.DeckStatsResponse actual = learningStatsService.getDeckStats(0);

            assertThat(actual.totalDecks()).isEqualTo(0L);
            assertThat(actual.byStatus()).isEmpty();
            assertThat(actual.topSystemDecks()).isEmpty();
            assertThat(actual.topUserDecks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getLessonStats")
    class GetLessonStatsTests {

        @Test
        @DisplayName("Should return cached response when present in Redis")
        void getLessonStats_CacheHit_ReturnsCached() throws Exception {
            String cacheKey = "admin:stats:learning:lessons:limit=10";
            String jsonContent = "{\"totalLessons\":30}";
            LearningStatsResponses.LessonStatsResponse expected = new LearningStatsResponses.LessonStatsResponse(
                    30L, List.of(), List.of(), List.of(), LocalDateTime.now()
            );

            when(redisService.get(cacheKey)).thenReturn(jsonContent);
            when(objectMapper.readValue(jsonContent, LearningStatsResponses.LessonStatsResponse.class)).thenReturn(expected);

            LearningStatsResponses.LessonStatsResponse actual = learningStatsService.getLessonStats(10);

            assertThat(actual).isEqualTo(expected);
            verifyNoInteractions(lessonStatsQueryService);
        }

        @Test
        @DisplayName("Should fetch distributions and top lessons, then cache on cache miss")
        void getLessonStats_CacheMiss_FetchesAndCaches() throws Exception {
            String cacheKey = "admin:stats:learning:lessons:limit=10";
            when(redisService.get(cacheKey)).thenReturn(null);

            LessonStatusCountProjection statusProj = mock(LessonStatusCountProjection.class);
            when(statusProj.getStatus()).thenReturn(LessonStatus.PUBLISHED);
            when(statusProj.getCountVal()).thenReturn(50L);
            when(lessonStatsQueryService.countLessonsByStatus()).thenReturn(List.of(statusProj));

            LessonPremiumCountProjection premiumProj = mock(LessonPremiumCountProjection.class);
            when(premiumProj.getIsPremium()).thenReturn(false);
            when(premiumProj.getCountVal()).thenReturn(50L);
            when(lessonStatsQueryService.countLessonsByPremium()).thenReturn(List.of(premiumProj));

            TopLessonProjection topProj = mock(TopLessonProjection.class);
            when(topProj.getLessonId()).thenReturn(10L);
            when(topProj.getTitle()).thenReturn("Lesson 1");
            when(topProj.getThumbnailUrl()).thenReturn("thumb.jpg");
            when(topProj.getStatus()).thenReturn("PUBLISHED");
            when(topProj.getIsPremium()).thenReturn(false);
            when(topProj.getLearnerCount()).thenReturn(200L);
            when(lessonStatsQueryService.getTopLessonsByCompletion(10)).thenReturn(List.of(topProj));

            when(objectMapper.writeValueAsString(any())).thenReturn("{\"cached\":true}");

            LearningStatsResponses.LessonStatsResponse actual = learningStatsService.getLessonStats(10);

            assertThat(actual.totalLessons()).isEqualTo(50L);
            assertThat(actual.byStatus()).hasSize(1);
            assertThat(actual.byStatus().get(0).category()).isEqualTo("PUBLISHED");
            assertThat(actual.byStatus().get(0).percentage()).isEqualTo(100.0);
            assertThat(actual.byPremium().get(0).category()).isEqualTo("FREE");
            assertThat(actual.topLessons()).hasSize(1);
            assertThat(actual.topLessons().get(0).learnerCount()).isEqualTo(200L);
            assertThat(actual.topLessons().get(0).thumbnailUrl()).isEqualTo("https://cdn.elingo.com/thumb.jpg");

            verify(redisService).save(eq(cacheKey), eq("{\"cached\":true}"), eq(5L));
        }

        @Test
        @DisplayName("Should handle zero total lessons without division by zero")
        void getLessonStats_ZeroTotalLessons_HandlesZeroPercentage() {
            when(redisService.get(anyString())).thenReturn(null);
            when(lessonStatsQueryService.countLessonsByStatus()).thenReturn(Collections.emptyList());
            when(lessonStatsQueryService.countLessonsByPremium()).thenReturn(Collections.emptyList());
            when(lessonStatsQueryService.getTopLessonsByCompletion(anyInt())).thenReturn(Collections.emptyList());

            LearningStatsResponses.LessonStatsResponse actual = learningStatsService.getLessonStats(-5);

            assertThat(actual.totalLessons()).isEqualTo(0L);
            assertThat(actual.byStatus()).isEmpty();
            assertThat(actual.byPremium()).isEmpty();
            assertThat(actual.topLessons()).isEmpty();
        }
    }
}
