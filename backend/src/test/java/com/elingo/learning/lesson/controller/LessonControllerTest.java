package com.elingo.learning.lesson.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.learning.lesson.dto.request.GetPublishedLessonsRequest;
import com.elingo.learning.lesson.dto.response.LessonDetailResponse;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.LessonResponse;
import com.elingo.learning.lesson.dto.response.LessonSegmentResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.entity.ProgressStatus;
import com.elingo.learning.lesson.service.LessonService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LessonControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LessonService lessonService;

    @InjectMocks
    private LessonController lessonController;

    private static final Long USER_ID = 1L;
    private static final Long LESSON_ID = 10L;
    private static final String SLUG = "ted-talk-on-habits";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(lessonController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(@NonNull MethodParameter parameter) {
                                return parameter.hasParameterAnnotation(CurrentUserId.class);
                            }

                            @Override
                            public Object resolveArgument(@NonNull MethodParameter parameter,
                                                          ModelAndViewContainer mavContainer,
                                                          @NonNull NativeWebRequest webRequest,
                                                          WebDataBinderFactory binderFactory) {
                                return USER_ID;
                            }
                        }
                )
                .build();
    }

    private LessonProgressSummaryResponse buildProgress() {
        return new LessonProgressSummaryResponse(
                ProgressStatus.IN_PROGRESS, new BigDecimal("50.00"), 12000,
                ProgressStatus.NOT_STARTED, BigDecimal.ZERO, null);
    }

    private LessonResponse buildLessonResponse(Long id, String slug, LessonProgressSummaryResponse progress) {
        return new LessonResponse(id, "Lesson " + id, slug, "Description", false, 180000,
                "https://www.youtube.com/watch?v=abc", null, LocalDateTime.now(),
                Set.of(), Set.of(), progress);
    }

    private LessonDetailResponse buildLessonDetail() {
        return new LessonDetailResponse(LESSON_ID, "Lesson 10", SLUG, "Description", false, 180000,
                "https://www.youtube.com/watch?v=abc", null, LocalDateTime.now(),
                Set.of(), Set.of(), buildProgress(), 12, 60, 55);
    }

    private LessonSegmentResponse buildSegment(Long id, int startMs, SegmentProgressResponse progress) {
        return new LessonSegmentResponse(id, startMs, startMs + 3000, "Hello, world!", "hello world", "Xin chao", progress);
    }

    // ========================================================================
    // getAllPublishedLessons
    // ========================================================================
    @Nested
    @DisplayName("getAllPublishedLessons")
    class GetAllPublishedLessonsTests {

        @Test
        @DisplayName("Get published lessons successfully with default paging")
        void getAllPublishedLessons_Success_DefaultPaging() throws Exception {
            Page<LessonResponse> page = new PageImpl<>(
                    List.of(buildLessonResponse(LESSON_ID, SLUG, buildProgress())), PageRequest.of(0, 9), 1);
            when(lessonService.getAllPublishedLessons(eq(USER_ID), any(GetPublishedLessonsRequest.class), any(Pageable.class)))
                    .thenReturn(PageResponse.of(page));

            mockMvc.perform(get("/lessons"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].id").value(LESSON_ID))
                    .andExpect(jsonPath("$.data.content[0].slug").value(SLUG))
                    .andExpect(jsonPath("$.data.content[0].isPremium").value(false))
                    .andExpect(jsonPath("$.data.content[0].userProgress.dictationStatus").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            ArgumentCaptor<GetPublishedLessonsRequest> requestCaptor = ArgumentCaptor.forClass(GetPublishedLessonsRequest.class);
            verify(lessonService).getAllPublishedLessons(eq(USER_ID), requestCaptor.capture(), pageableCaptor.capture());

            assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(9);
            assertThat(pageableCaptor.getValue().getSort().getOrderFor("publishedAt").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
            assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
            assertThat(requestCaptor.getValue().cefrLevelIds()).isNullOrEmpty();
            assertThat(requestCaptor.getValue().tagIds()).isNullOrEmpty();
            assertThat(requestCaptor.getValue().keyword()).isNull();
        }

        @Test
        @DisplayName("Get published lessons successfully with filters and custom page size")
        void getAllPublishedLessons_Success_WithFilters() throws Exception {
            Page<LessonResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
            when(lessonService.getAllPublishedLessons(eq(USER_ID), any(GetPublishedLessonsRequest.class), any(Pageable.class)))
                    .thenReturn(PageResponse.of(page));

            mockMvc.perform(get("/lessons")
                            .param("cefrLevelIds", "1", "2")
                            .param("tagIds", "3")
                            .param("keyword", "habits")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(0));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            ArgumentCaptor<GetPublishedLessonsRequest> requestCaptor = ArgumentCaptor.forClass(GetPublishedLessonsRequest.class);
            verify(lessonService).getAllPublishedLessons(eq(USER_ID), requestCaptor.capture(), pageableCaptor.capture());

            assertThat(requestCaptor.getValue().cefrLevelIds()).containsExactlyInAnyOrder(1L, 2L);
            assertThat(requestCaptor.getValue().tagIds()).containsExactly(3L);
            assertThat(requestCaptor.getValue().keyword()).isEqualTo("habits");
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("Get published lessons failed: Keyword is longer than 255 characters")
        void getAllPublishedLessons_Fail_KeywordTooLong() throws Exception {
            mockMvc.perform(get("/lessons").param("keyword", "a".repeat(256)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("KEYWORD_INVALID"));

            verify(lessonService, never()).getAllPublishedLessons(any(), any(), any());
        }

        @Test
        @DisplayName("Get published lessons failed: Too many CEFR level filters")
        void getAllPublishedLessons_Fail_TooManyCefrLevels() throws Exception {
            mockMvc.perform(get("/lessons").param("cefrLevelIds", "1", "2", "3", "4", "5", "6", "7"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(lessonService, never()).getAllPublishedLessons(any(), any(), any());
        }

        @Test
        @DisplayName("Get published lessons failed: Tag id is not positive")
        void getAllPublishedLessons_Fail_NegativeTagId() throws Exception {
            mockMvc.perform(get("/lessons").param("tagIds", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(lessonService, never()).getAllPublishedLessons(any(), any(), any());
        }
    }

    // ========================================================================
    // getLessonById
    // ========================================================================
    @Nested
    @DisplayName("getLessonById")
    class GetLessonByIdTests {

        @Test
        @DisplayName("Get lesson by id successfully")
        void getLessonById_Success() throws Exception {
            when(lessonService.getLessonById(USER_ID, LESSON_ID)).thenReturn(buildLessonDetail());

            mockMvc.perform(get("/lessons/{lessonId}", LESSON_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(LESSON_ID))
                    .andExpect(jsonPath("$.data.slug").value(SLUG))
                    .andExpect(jsonPath("$.data.segmentCount").value(12))
                    .andExpect(jsonPath("$.data.dictationPassScore").value(60))
                    .andExpect(jsonPath("$.data.shadowingPassScore").value(55))
                    .andExpect(jsonPath("$.data.userProgress.dictationProgressPct").value(50.0));

            verify(lessonService).getLessonById(USER_ID, LESSON_ID);
        }

        @Test
        @DisplayName("Get lesson by id failed: Lesson id is not positive")
        void getLessonById_Fail_InvalidId() throws Exception {
            mockMvc.perform(get("/lessons/{lessonId}", -1))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(lessonService, never()).getLessonById(any(), any());
        }

        @Test
        @DisplayName("Get lesson by id failed: Lesson id is zero")
        void getLessonById_Fail_ZeroId() throws Exception {
            mockMvc.perform(get("/lessons/{lessonId}", 0))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(lessonService, never()).getLessonById(any(), any());
        }

        @Test
        @DisplayName("Get lesson by id failed: Lesson not found")
        void getLessonById_Fail_NotFound() throws Exception {
            when(lessonService.getLessonById(USER_ID, LESSON_ID))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            mockMvc.perform(get("/lessons/{lessonId}", LESSON_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get lesson by id failed: Premium required")
        void getLessonById_Fail_PremiumRequired() throws Exception {
            when(lessonService.getLessonById(USER_ID, LESSON_ID))
                    .thenThrow(new AppException(AppError.PREMIUM_REQUIRED));

            mockMvc.perform(get("/lessons/{lessonId}", LESSON_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("PREMIUM_REQUIRED"));
        }
    }

    // ========================================================================
    // getSegmentsByLessonId
    // ========================================================================
    @Nested
    @DisplayName("getSegmentsByLessonId")
    class GetSegmentsByLessonIdTests {

        @Test
        @DisplayName("Get segments by lesson id successfully")
        void getSegmentsByLessonId_Success() throws Exception {
            SegmentProgressResponse progress = new SegmentProgressResponse(
                    100L, 2, new BigDecimal("90.00"), 1, 1, new BigDecimal("70.00"), null, List.of());
            when(lessonService.getSegmentsByLessonId(USER_ID, LESSON_ID))
                    .thenReturn(List.of(buildSegment(100L, 0, progress), buildSegment(101L, 3000, null)));

            mockMvc.perform(get("/lessons/{lessonId}/segments", LESSON_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(100))
                    .andExpect(jsonPath("$.data[0].startMs").value(0))
                    .andExpect(jsonPath("$.data[0].transcriptNormalized").value("hello world"))
                    .andExpect(jsonPath("$.data[0].userProgress.dictationBestScore").value(90.0))
                    .andExpect(jsonPath("$.data[1].startMs").value(3000))
                    .andExpect(jsonPath("$.data[1].userProgress").doesNotExist());

            verify(lessonService).getSegmentsByLessonId(USER_ID, LESSON_ID);
        }

        @Test
        @DisplayName("Get segments by lesson id successfully when lesson has no segments")
        void getSegmentsByLessonId_Success_Empty() throws Exception {
            when(lessonService.getSegmentsByLessonId(USER_ID, LESSON_ID)).thenReturn(List.of());

            mockMvc.perform(get("/lessons/{lessonId}/segments", LESSON_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("Get segments by lesson id failed: Lesson id is not positive")
        void getSegmentsByLessonId_Fail_InvalidId() throws Exception {
            mockMvc.perform(get("/lessons/{lessonId}/segments", -5))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(lessonService, never()).getSegmentsByLessonId(any(), any());
        }

        @Test
        @DisplayName("Get segments by lesson id failed: Lesson not found")
        void getSegmentsByLessonId_Fail_NotFound() throws Exception {
            when(lessonService.getSegmentsByLessonId(USER_ID, LESSON_ID))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            mockMvc.perform(get("/lessons/{lessonId}/segments", LESSON_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_NOT_FOUND"));
        }
    }

    // ========================================================================
    // getLessonBySlug
    // ========================================================================
    @Nested
    @DisplayName("getLessonBySlug")
    class GetLessonBySlugTests {

        @Test
        @DisplayName("Get lesson by slug successfully")
        void getLessonBySlug_Success() throws Exception {
            when(lessonService.getLessonBySlug(USER_ID, SLUG)).thenReturn(buildLessonDetail());

            mockMvc.perform(get("/lessons/slug/{slug}", SLUG))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(LESSON_ID))
                    .andExpect(jsonPath("$.data.slug").value(SLUG));

            verify(lessonService).getLessonBySlug(USER_ID, SLUG);
        }

        @Test
        @DisplayName("Get lesson by slug failed: Lesson not found")
        void getLessonBySlug_Fail_NotFound() throws Exception {
            when(lessonService.getLessonBySlug(USER_ID, "unknown"))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            mockMvc.perform(get("/lessons/slug/{slug}", "unknown"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get lesson by slug failed: Premium required")
        void getLessonBySlug_Fail_PremiumRequired() throws Exception {
            when(lessonService.getLessonBySlug(USER_ID, SLUG))
                    .thenThrow(new AppException(AppError.PREMIUM_REQUIRED));

            mockMvc.perform(get("/lessons/slug/{slug}", SLUG))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0].code").value("PREMIUM_REQUIRED"));
        }
    }

    // ========================================================================
    // getSegmentsByLessonSlug
    // ========================================================================
    @Nested
    @DisplayName("getSegmentsByLessonSlug")
    class GetSegmentsByLessonSlugTests {

        @Test
        @DisplayName("Get segments by lesson slug successfully")
        void getSegmentsByLessonSlug_Success() throws Exception {
            when(lessonService.getSegmentsByLessonSlug(USER_ID, SLUG))
                    .thenReturn(List.of(buildSegment(100L, 0, null)));

            mockMvc.perform(get("/lessons/slug/{slug}/segments", SLUG))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(100))
                    .andExpect(jsonPath("$.data[0].translation").value("Xin chao"));

            verify(lessonService).getSegmentsByLessonSlug(USER_ID, SLUG);
        }

        @Test
        @DisplayName("Get segments by lesson slug failed: Lesson not found")
        void getSegmentsByLessonSlug_Fail_NotFound() throws Exception {
            when(lessonService.getSegmentsByLessonSlug(USER_ID, "unknown"))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            mockMvc.perform(get("/lessons/slug/{slug}/segments", "unknown"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_NOT_FOUND"));
        }
    }
}
