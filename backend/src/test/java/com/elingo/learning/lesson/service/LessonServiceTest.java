package com.elingo.learning.lesson.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.lesson.config.LessonProperties;
import com.elingo.learning.lesson.dto.request.GetPublishedLessonsRequest;
import com.elingo.learning.lesson.dto.response.LessonDetailResponse;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.LessonResponse;
import com.elingo.learning.lesson.dto.response.LessonSegmentResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.learning.lesson.entity.ProgressStatus;
import com.elingo.learning.lesson.mapper.LessonMapper;
import com.elingo.learning.lesson.mapper.LessonSegmentMapper;
import com.elingo.learning.lesson.repository.LessonRepository;
import com.elingo.learning.lesson.repository.LessonSegmentRepository;
import com.elingo.learning.lesson.service.impl.LessonServiceImpl;
import com.elingo.progress.service.ProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonSegmentRepository lessonSegmentRepository;

    @Mock
    private LessonAccessService lessonAccessService;

    @Mock
    private LessonMapper lessonMapper;

    @Mock
    private LessonSegmentMapper lessonSegmentMapper;

    @Mock
    private ProgressService progressService;

    @InjectMocks
    private LessonServiceImpl lessonService;

    private static final Long USER_ID = 1L;
    private static final Long LESSON_ID = 10L;
    private static final String SLUG = "ted-talk-on-habits";
    private static final Pageable PAGEABLE = PageRequest.of(0, 9);
    private static final LessonProperties PROPS = new LessonProperties(60, 55, 5, 10);

    private Lesson lesson1;
    private Lesson lesson2;
    private LessonProgressSummaryResponse progressSummary;

    @BeforeEach
    void setUp() {
        // LessonProperties là record nên không @Mock được -> gán trực tiếp
        ReflectionTestUtils.setField(lessonService, "lessonProperties", PROPS);

        lesson1 = buildLesson(LESSON_ID, SLUG);
        lesson2 = buildLesson(LESSON_ID + 1, "another-lesson");
        progressSummary = new LessonProgressSummaryResponse(
                ProgressStatus.IN_PROGRESS, new BigDecimal("50.00"), 3000,
                ProgressStatus.NOT_STARTED, BigDecimal.ZERO, null);
    }

    private Lesson buildLesson(Long id, String slug) {
        return Lesson.builder()
                .id(id)
                .title("Lesson " + id)
                .slug(slug)
                .status(LessonStatus.PUBLISHED)
                .isPremium(false)
                .publishedAt(LocalDateTime.now())
                .sourceUrl("https://www.youtube.com/watch?v=abc")
                .build();
    }

    private LessonResponse buildLessonResponse(Lesson lesson, LessonProgressSummaryResponse progress) {
        return new LessonResponse(lesson.getId(), lesson.getTitle(), lesson.getSlug(), null, false, null,
                lesson.getSourceUrl(), null, lesson.getPublishedAt(), Set.of(), Set.of(), progress);
    }

    private LessonDetailResponse buildLessonDetail(LessonProgressSummaryResponse progress, int segmentCount) {
        return new LessonDetailResponse(LESSON_ID, "Lesson 10", SLUG, null, false, null,
                "https://www.youtube.com/watch?v=abc", null, LocalDateTime.now(),
                Set.of(), Set.of(), progress, segmentCount, PROPS.dictationPassScore(), PROPS.shadowingPassScore());
    }

    private LessonSegment buildSegment(Long id, int startMs) {
        return LessonSegment.builder()
                .id(id)
                .lesson(lesson1)
                .startMs(startMs)
                .endMs(startMs + 3000)
                .transcriptOriginal("Hello, world!")
                .transcriptNormalized("hello world")
                .build();
    }

    private LessonSegmentResponse buildSegmentResponse(LessonSegment segment, SegmentProgressResponse progress) {
        return new LessonSegmentResponse(segment.getId(), segment.getStartMs(), segment.getEndMs(),
                segment.getTranscriptOriginal(), segment.getTranscriptNormalized(), null, progress);
    }

    private SegmentProgressResponse buildSegmentProgress(Long segmentId) {
        return new SegmentProgressResponse(segmentId, 1, new BigDecimal("80.00"), 0, 0, BigDecimal.ZERO, null, List.of());
    }

    // ========================================================================
    // getAllPublishedLessons
    // ========================================================================
    @Nested
    @DisplayName("getAllPublishedLessons")
    class GetAllPublishedLessonsTests {

        private GetPublishedLessonsRequest emptyRequest() {
            return new GetPublishedLessonsRequest(null, null, null);
        }

        @Test
        @DisplayName("Get published lessons successfully with user progress")
        void getAllPublishedLessons_Success_WithProgress() {
            Page<Lesson> page = new PageImpl<>(List.of(lesson1, lesson2), PAGEABLE, 2);
            LessonResponse response1 = buildLessonResponse(lesson1, progressSummary);
            LessonResponse response2 = buildLessonResponse(lesson2, null);

            when(lessonRepository.findAll(any(Specification.class), eq(PAGEABLE))).thenReturn(page);
            // Chỉ lesson1 có tiến độ học
            when(progressService.getLessonProgressSummaryMap(USER_ID, List.of(LESSON_ID, LESSON_ID + 1)))
                    .thenReturn(Map.of(LESSON_ID, progressSummary));
            when(lessonMapper.toLessonResponse(lesson1, progressSummary)).thenReturn(response1);
            when(lessonMapper.toLessonResponse(lesson2, null)).thenReturn(response2);

            PageResponse<LessonResponse> result = lessonService.getAllPublishedLessons(USER_ID, emptyRequest(), PAGEABLE);

            assertThat(result.content()).containsExactly(response1, response2);
            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.page()).isEqualTo(1);
        }

        @Test
        @DisplayName("Get published lessons successfully for anonymous user without querying progress")
        void getAllPublishedLessons_Success_Anonymous() {
            Page<Lesson> page = new PageImpl<>(List.of(lesson1), PAGEABLE, 1);
            LessonResponse response1 = buildLessonResponse(lesson1, null);

            when(lessonRepository.findAll(any(Specification.class), eq(PAGEABLE))).thenReturn(page);
            when(lessonMapper.toLessonResponse(lesson1, null)).thenReturn(response1);

            PageResponse<LessonResponse> result = lessonService.getAllPublishedLessons(null, emptyRequest(), PAGEABLE);

            assertThat(result.content()).containsExactly(response1);
            verifyNoInteractions(progressService);
        }

        @Test
        @DisplayName("Get published lessons successfully with filters")
        void getAllPublishedLessons_Success_WithFilters() {
            GetPublishedLessonsRequest request = new GetPublishedLessonsRequest(Set.of(1L, 2L), Set.of(3L), "  habits  ");
            Page<Lesson> page = new PageImpl<>(List.of(lesson1), PAGEABLE, 1);
            LessonResponse response1 = buildLessonResponse(lesson1, progressSummary);

            when(lessonRepository.findAll(any(Specification.class), eq(PAGEABLE))).thenReturn(page);
            when(progressService.getLessonProgressSummaryMap(USER_ID, List.of(LESSON_ID)))
                    .thenReturn(Map.of(LESSON_ID, progressSummary));
            when(lessonMapper.toLessonResponse(lesson1, progressSummary)).thenReturn(response1);

            PageResponse<LessonResponse> result = lessonService.getAllPublishedLessons(USER_ID, request, PAGEABLE);

            assertThat(result.content()).containsExactly(response1);
            verify(lessonRepository).findAll(any(Specification.class), eq(PAGEABLE));
        }

        @Test
        @DisplayName("Get published lessons successfully with blank keyword")
        void getAllPublishedLessons_Success_BlankKeyword() {
            GetPublishedLessonsRequest request = new GetPublishedLessonsRequest(Set.of(), Set.of(), "   ");
            Page<Lesson> page = new PageImpl<>(List.of(), PAGEABLE, 0);

            when(lessonRepository.findAll(any(Specification.class), eq(PAGEABLE))).thenReturn(page);
            when(progressService.getLessonProgressSummaryMap(eq(USER_ID), anyCollection())).thenReturn(Map.of());

            PageResponse<LessonResponse> result = lessonService.getAllPublishedLessons(USER_ID, request, PAGEABLE);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("Get published lessons successfully when no lesson matches")
        void getAllPublishedLessons_Success_Empty() {
            Page<Lesson> page = new PageImpl<>(List.of(), PAGEABLE, 0);

            when(lessonRepository.findAll(any(Specification.class), eq(PAGEABLE))).thenReturn(page);
            when(progressService.getLessonProgressSummaryMap(eq(USER_ID), anyCollection())).thenReturn(Map.of());

            PageResponse<LessonResponse> result = lessonService.getAllPublishedLessons(USER_ID, emptyRequest(), PAGEABLE);

            assertThat(result.content()).isEmpty();
            verify(lessonMapper, never()).toLessonResponse(any(), any());
        }
    }

    // ========================================================================
    // getLessonById / getLessonBySlug
    // ========================================================================
    @Nested
    @DisplayName("getLessonById")
    class GetLessonByIdTests {

        @Test
        @DisplayName("Get lesson by id successfully with user progress")
        void getLessonById_Success_WithProgress() {
            LessonDetailResponse detail = buildLessonDetail(progressSummary, 12);
            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID)).thenReturn(lesson1);
            when(lessonSegmentRepository.countByLessonId(LESSON_ID)).thenReturn(12L);
            when(progressService.findLessonProgressSummary(USER_ID, LESSON_ID)).thenReturn(Optional.of(progressSummary));
            when(lessonMapper.toLessonDetailResponse(lesson1, 12, progressSummary, 60, 55)).thenReturn(detail);

            LessonDetailResponse result = lessonService.getLessonById(USER_ID, LESSON_ID);

            assertThat(result).isSameAs(detail);
            assertThat(result.segmentCount()).isEqualTo(12);
            assertThat(result.dictationPassScore()).isEqualTo(60);
            assertThat(result.shadowingPassScore()).isEqualTo(55);
        }

        @Test
        @DisplayName("Get lesson by id successfully when user has no progress yet")
        void getLessonById_Success_NoProgress() {
            LessonDetailResponse detail = buildLessonDetail(null, 5);
            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID)).thenReturn(lesson1);
            when(lessonSegmentRepository.countByLessonId(LESSON_ID)).thenReturn(5L);
            when(progressService.findLessonProgressSummary(USER_ID, LESSON_ID)).thenReturn(Optional.empty());
            when(lessonMapper.toLessonDetailResponse(lesson1, 5, null, 60, 55)).thenReturn(detail);

            LessonDetailResponse result = lessonService.getLessonById(USER_ID, LESSON_ID);

            assertThat(result.userProgress()).isNull();
            assertThat(result.segmentCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Get lesson by id successfully for anonymous user without querying progress")
        void getLessonById_Success_Anonymous() {
            LessonDetailResponse detail = buildLessonDetail(null, 5);
            when(lessonAccessService.validateLessonAccess(null, LESSON_ID)).thenReturn(lesson1);
            when(lessonSegmentRepository.countByLessonId(LESSON_ID)).thenReturn(5L);
            when(lessonMapper.toLessonDetailResponse(lesson1, 5, null, 60, 55)).thenReturn(detail);

            LessonDetailResponse result = lessonService.getLessonById(null, LESSON_ID);

            assertThat(result).isSameAs(detail);
            verifyNoInteractions(progressService);
        }

        @Test
        @DisplayName("Get lesson by id failed: Lesson not found")
        void getLessonById_Fail_NotFound() {
            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            assertThatThrownBy(() -> lessonService.getLessonById(USER_ID, LESSON_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_NOT_FOUND));

            verifyNoInteractions(lessonSegmentRepository);
            verifyNoInteractions(lessonMapper);
        }

        @Test
        @DisplayName("Get lesson by id failed: Premium required")
        void getLessonById_Fail_PremiumRequired() {
            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID))
                    .thenThrow(new AppException(AppError.PREMIUM_REQUIRED));

            assertThatThrownBy(() -> lessonService.getLessonById(USER_ID, LESSON_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PREMIUM_REQUIRED));

            verifyNoInteractions(lessonMapper);
        }
    }

    @Nested
    @DisplayName("getLessonBySlug")
    class GetLessonBySlugTests {

        @Test
        @DisplayName("Get lesson by slug successfully with user progress")
        void getLessonBySlug_Success() {
            LessonDetailResponse detail = buildLessonDetail(progressSummary, 12);
            when(lessonAccessService.validateLessonAccess(USER_ID, SLUG)).thenReturn(lesson1);
            when(lessonSegmentRepository.countByLessonId(LESSON_ID)).thenReturn(12L);
            when(progressService.findLessonProgressSummary(USER_ID, LESSON_ID)).thenReturn(Optional.of(progressSummary));
            when(lessonMapper.toLessonDetailResponse(lesson1, 12, progressSummary, 60, 55)).thenReturn(detail);

            LessonDetailResponse result = lessonService.getLessonBySlug(USER_ID, SLUG);

            assertThat(result).isSameAs(detail);
        }

        @Test
        @DisplayName("Get lesson by slug successfully for anonymous user")
        void getLessonBySlug_Success_Anonymous() {
            LessonDetailResponse detail = buildLessonDetail(null, 3);
            when(lessonAccessService.validateLessonAccess(null, SLUG)).thenReturn(lesson1);
            when(lessonSegmentRepository.countByLessonId(LESSON_ID)).thenReturn(3L);
            when(lessonMapper.toLessonDetailResponse(lesson1, 3, null, 60, 55)).thenReturn(detail);

            LessonDetailResponse result = lessonService.getLessonBySlug(null, SLUG);

            assertThat(result).isSameAs(detail);
            verifyNoInteractions(progressService);
        }

        @Test
        @DisplayName("Get lesson by slug failed: Lesson not found")
        void getLessonBySlug_Fail_NotFound() {
            when(lessonAccessService.validateLessonAccess(USER_ID, SLUG))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            assertThatThrownBy(() -> lessonService.getLessonBySlug(USER_ID, SLUG))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_NOT_FOUND));
        }
    }

    // ========================================================================
    // getSegmentsByLessonId / getSegmentsByLessonSlug
    // ========================================================================
    @Nested
    @DisplayName("getSegmentsByLessonId")
    class GetSegmentsByLessonIdTests {

        @Test
        @DisplayName("Get segments successfully with progress for the segments the user has studied")
        void getSegmentsByLessonId_Success_WithProgress() {
            LessonSegment segment1 = buildSegment(100L, 0);
            LessonSegment segment2 = buildSegment(101L, 3000);
            SegmentProgressResponse progress1 = buildSegmentProgress(100L);
            LessonSegmentResponse response1 = buildSegmentResponse(segment1, progress1);
            LessonSegmentResponse response2 = buildSegmentResponse(segment2, null);

            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID)).thenReturn(lesson1);
            when(lessonSegmentRepository.findAllByLessonIdOrderByStartMsAsc(LESSON_ID))
                    .thenReturn(List.of(segment1, segment2));
            when(progressService.getSegmentProgressMap(USER_ID, LESSON_ID)).thenReturn(Map.of(100L, progress1));
            when(lessonSegmentMapper.toLessonSegmentResponse(segment1, progress1)).thenReturn(response1);
            when(lessonSegmentMapper.toLessonSegmentResponse(segment2, null)).thenReturn(response2);

            List<LessonSegmentResponse> result = lessonService.getSegmentsByLessonId(USER_ID, LESSON_ID);

            // Giữ nguyên thứ tự startMs từ repository
            assertThat(result).containsExactly(response1, response2);
        }

        @Test
        @DisplayName("Get segments successfully for anonymous user without querying progress")
        void getSegmentsByLessonId_Success_Anonymous() {
            LessonSegment segment1 = buildSegment(100L, 0);
            LessonSegmentResponse response1 = buildSegmentResponse(segment1, null);

            when(lessonAccessService.validateLessonAccess(null, LESSON_ID)).thenReturn(lesson1);
            when(lessonSegmentRepository.findAllByLessonIdOrderByStartMsAsc(LESSON_ID)).thenReturn(List.of(segment1));
            when(lessonSegmentMapper.toLessonSegmentResponse(segment1, null)).thenReturn(response1);

            List<LessonSegmentResponse> result = lessonService.getSegmentsByLessonId(null, LESSON_ID);

            assertThat(result).containsExactly(response1);
            verifyNoInteractions(progressService);
        }

        @Test
        @DisplayName("Get segments successfully when lesson has no segments")
        void getSegmentsByLessonId_Success_Empty() {
            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID)).thenReturn(lesson1);
            when(lessonSegmentRepository.findAllByLessonIdOrderByStartMsAsc(LESSON_ID)).thenReturn(List.of());

            List<LessonSegmentResponse> result = lessonService.getSegmentsByLessonId(USER_ID, LESSON_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(progressService);
            verifyNoInteractions(lessonSegmentMapper);
        }

        @Test
        @DisplayName("Get segments failed: Lesson not found")
        void getSegmentsByLessonId_Fail_NotFound() {
            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            assertThatThrownBy(() -> lessonService.getSegmentsByLessonId(USER_ID, LESSON_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_NOT_FOUND));

            verifyNoInteractions(lessonSegmentRepository);
        }
    }

    @Nested
    @DisplayName("getSegmentsByLessonSlug")
    class GetSegmentsByLessonSlugTests {

        @Test
        @DisplayName("Get segments by slug successfully")
        void getSegmentsByLessonSlug_Success() {
            LessonSegment segment1 = buildSegment(100L, 0);
            SegmentProgressResponse progress1 = buildSegmentProgress(100L);
            LessonSegmentResponse response1 = buildSegmentResponse(segment1, progress1);

            when(lessonAccessService.validateLessonAccess(USER_ID, SLUG)).thenReturn(lesson1);
            when(lessonSegmentRepository.findAllByLessonIdOrderByStartMsAsc(LESSON_ID)).thenReturn(List.of(segment1));
            when(progressService.getSegmentProgressMap(USER_ID, LESSON_ID)).thenReturn(Map.of(100L, progress1));
            when(lessonSegmentMapper.toLessonSegmentResponse(segment1, progress1)).thenReturn(response1);

            List<LessonSegmentResponse> result = lessonService.getSegmentsByLessonSlug(USER_ID, SLUG);

            assertThat(result).containsExactly(response1);
        }

        @Test
        @DisplayName("Get segments by slug successfully when lesson has no segments")
        void getSegmentsByLessonSlug_Success_Empty() {
            when(lessonAccessService.validateLessonAccess(USER_ID, SLUG)).thenReturn(lesson1);
            when(lessonSegmentRepository.findAllByLessonIdOrderByStartMsAsc(LESSON_ID)).thenReturn(List.of());

            assertThat(lessonService.getSegmentsByLessonSlug(USER_ID, SLUG)).isEmpty();
        }

        @Test
        @DisplayName("Get segments by slug failed: Lesson not found")
        void getSegmentsByLessonSlug_Fail_NotFound() {
            when(lessonAccessService.validateLessonAccess(USER_ID, SLUG))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            assertThatThrownBy(() -> lessonService.getSegmentsByLessonSlug(USER_ID, SLUG))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_NOT_FOUND));
        }
    }
}
