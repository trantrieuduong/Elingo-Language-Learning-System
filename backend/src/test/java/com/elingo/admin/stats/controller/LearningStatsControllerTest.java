package com.elingo.admin.stats.controller;

import com.elingo.admin.stats.dto.response.CategoryShare;
import com.elingo.admin.stats.dto.response.LearningStatsResponses;
import com.elingo.admin.stats.service.LearningStatsService;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.common.exception.HttpExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LearningStatsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LearningStatsService learningStatsService;

    @InjectMocks
    private LearningStatsController learningStatsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(learningStatsController)
                .setControllerAdvice(new GlobalExceptionHandler(), new HttpExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("getLearningActivity")
    class GetLearningActivityTests {

        @Test
        @DisplayName("Should return 200 and activity data when request parameters are valid")
        void getLearningActivity_Success() throws Exception {
            LearningStatsResponses.LearningActivityResponse response = new LearningStatsResponses.LearningActivityResponse(
                    new LearningStatsResponses.ActivitySourceTotals(10L, 20L, 15L, 30L, 25L, 100L),
                    List.of(new LearningStatsResponses.LearningActivityPoint(
                            LocalDate.of(2026, 11, 1), 10L, 20L, 15L, 30L, 25L, 100L
                    )),
                    LocalDateTime.now()
            );

            when(learningStatsService.getLearningActivity(any())).thenReturn(response);

            mockMvc.perform(get("/admin/stats/learning/activity")
                            .param("from", "2026-11-01")
                            .param("to", "2026-11-03")
                            .param("granularity", "DAY")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totals.grandTotal", is(100)))
                    .andExpect(jsonPath("$.data.points", hasSize(1)))
                    .andExpect(jsonPath("$.data.points[0].flashcard", is(10)));

            verify(learningStatsService).getLearningActivity(any());
        }

        @Test
        @DisplayName("Should return 400 when from date is after to date")
        void getLearningActivity_InvalidDateRange_Returns400() throws Exception {
            mockMvc.perform(get("/admin/stats/learning/activity")
                            .param("from", "2026-11-20")
                            .param("to", "2026-11-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }
    }

    @Nested
    @DisplayName("getDeckStats")
    class GetDeckStatsTests {

        @Test
        @DisplayName("Should return 200 and deck distribution with default limit")
        void getDeckStats_Success_DefaultLimit() throws Exception {
            LearningStatsResponses.DeckStatsResponse response = new LearningStatsResponses.DeckStatsResponse(
                    50L,
                    List.of(new CategoryShare<>("PUBLISHED", 40L, 80.0)),
                    List.of(new CategoryShare<>("SYSTEM", 30L, 60.0)),
                    List.of(new CategoryShare<>("FREE", 45L, 90.0)),
                    List.of(new LearningStatsResponses.TopDeckItem(1L, "Oxford", null, "PUBLISHED", "SYSTEM", false, 300L)),
                    List.of(new LearningStatsResponses.TopDeckItem(2L, "Community", null, "PUBLISHED", "USER", false, 80L)),
                    LocalDateTime.now()
            );

            when(learningStatsService.getDeckStats(eq(10))).thenReturn(response);

            mockMvc.perform(get("/admin/stats/learning/decks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalDecks", is(50)))
                    .andExpect(jsonPath("$.data.byStatus", hasSize(1)))
                    .andExpect(jsonPath("$.data.topSystemDecks", hasSize(1)))
                    .andExpect(jsonPath("$.data.topUserDecks", hasSize(1)));

            verify(learningStatsService).getDeckStats(10);
        }

        @Test
        @DisplayName("Should return 200 when custom limit parameter is passed")
        void getDeckStats_Success_CustomLimit() throws Exception {
            LearningStatsResponses.DeckStatsResponse response = new LearningStatsResponses.DeckStatsResponse(
                    0L, List.of(), List.of(), List.of(), List.of(), List.of(), LocalDateTime.now()
            );

            when(learningStatsService.getDeckStats(eq(5))).thenReturn(response);

            mockMvc.perform(get("/admin/stats/learning/decks").param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));

            verify(learningStatsService).getDeckStats(5);
        }
    }

    @Nested
    @DisplayName("getLessonStats")
    class GetLessonStatsTests {

        @Test
        @DisplayName("Should return 200 and lesson distribution with top completed lessons")
        void getLessonStats_Success() throws Exception {
            LearningStatsResponses.LessonStatsResponse response = new LearningStatsResponses.LessonStatsResponse(
                    30L,
                    List.of(new CategoryShare<>("PUBLISHED", 30L, 100.0)),
                    List.of(new CategoryShare<>("FREE", 25L, 83.33)),
                    List.of(new LearningStatsResponses.TopLessonItem(10L, "Coffee Conversation", null, "PUBLISHED", false, 150L)),
                    LocalDateTime.now()
            );

            when(learningStatsService.getLessonStats(eq(10))).thenReturn(response);

            mockMvc.perform(get("/admin/stats/learning/lessons"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalLessons", is(30)))
                    .andExpect(jsonPath("$.data.topLessons", hasSize(1)))
                    .andExpect(jsonPath("$.data.topLessons[0].learnerCount", is(150)));

            verify(learningStatsService).getLessonStats(10);
        }
    }
}
