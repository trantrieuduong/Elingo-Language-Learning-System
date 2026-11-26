package com.elingo.learning.lesson.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.lesson.dto.request.CreateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.request.DeleteLessonSegmentsRequest;
import com.elingo.learning.lesson.dto.request.UpdateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonSegmentResponse;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.learning.lesson.mapper.LessonSegmentMapper;
import com.elingo.learning.lesson.repository.LessonRepository;
import com.elingo.learning.lesson.repository.LessonSegmentRepository;
import com.elingo.learning.lesson.repository.SegmentWordAccuracyRepository;
import com.elingo.learning.lesson.repository.UserSegmentProgressRepository;
import com.elingo.learning.lesson.service.impl.LessonSegmentServiceImpl;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("LessonSegmentService")
class LessonSegmentServiceTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonSegmentRepository lessonSegmentRepository;
    @Mock
    private UserSegmentProgressRepository userSegmentProgressRepository;
    @Mock
    private SegmentWordAccuracyRepository segmentWordAccuracyRepository;
    @Mock
    private LessonSegmentMapper lessonSegmentMapper;

    @InjectMocks
    private LessonSegmentServiceImpl lessonSegmentService;

    private static final Long LESSON_ID = 7L;
    private static final Long SEGMENT_ID = 11L;

    private Lesson draftLesson;
    private Lesson publishedLesson;
    private LessonSegment segment;
    private AdminLessonSegmentResponse segmentResponse;

    @BeforeEach
    void setUp() {
        draftLesson = Lesson.builder()
                .id(LESSON_ID)
                .title("Coffee Lesson")
                .slug("coffee-lesson-12345678")
                .status(LessonStatus.DRAFT)
                .durationMs(60_000)
                .build();

        publishedLesson = Lesson.builder()
                .id(LESSON_ID)
                .title("Coffee Lesson")
                .slug("coffee-lesson-12345678")
                .status(LessonStatus.PUBLISHED)
                .durationMs(60_000)
                .build();

        segment = LessonSegment.builder()
                .id(SEGMENT_ID)
                .lesson(draftLesson)
                .startMs(1000)
                .endMs(5000)
                .transcriptOriginal("Hi, can I get a coffee?")
                .transcriptNormalized("hi can i get a coffee")
                .translation("Xin chào, cho tôi một ly cà phê?")
                .build();

        segmentResponse = new AdminLessonSegmentResponse(
                SEGMENT_ID, LESSON_ID, 1000, 5000,
                "Hi, can I get a coffee?", "hi can i get a coffee",
                "Xin chào, cho tôi một ly cà phê?",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("getSegmentsByLessonIdForAdmin")
    class GetSegmentsByLessonIdForAdmin {

        @Test
        @DisplayName("should return mapped segments ordered by startMs when lesson exists")
        void shouldReturnSegments_whenLessonExists() {
            // given
            when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
            when(lessonSegmentRepository.findAllByLessonIdOrderByStartMsAsc(LESSON_ID)).thenReturn(List.of(segment));
            when(lessonSegmentMapper.toAdminLessonSegmentResponse(segment)).thenReturn(segmentResponse);

            // when
            List<AdminLessonSegmentResponse> result = lessonSegmentService.getSegmentsByLessonIdForAdmin(LESSON_ID);

            // then
            assertThat(result).containsExactly(segmentResponse);
            verify(lessonSegmentRepository).findAllByLessonIdOrderByStartMsAsc(LESSON_ID);
        }

        @Test
        @DisplayName("should return empty list when lesson has no segments")
        void shouldReturnEmptyList_whenNoSegments() {
            // given
            when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
            when(lessonSegmentRepository.findAllByLessonIdOrderByStartMsAsc(LESSON_ID)).thenReturn(List.of());

            // when
            List<AdminLessonSegmentResponse> result = lessonSegmentService.getSegmentsByLessonIdForAdmin(LESSON_ID);

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(lessonSegmentMapper);
        }

        @Test
        @DisplayName("should throw LESSON_NOT_FOUND when lesson does not exist")
        void shouldThrowLessonNotFound_whenLessonMissing() {
            // given
            when(lessonRepository.existsById(LESSON_ID)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.getSegmentsByLessonIdForAdmin(LESSON_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.LESSON_NOT_FOUND));

            verifyNoInteractions(lessonSegmentRepository);
        }
    }

    @Nested
    @DisplayName("createSegment")
    class CreateSegment {

        @Test
        @DisplayName("should save segment with normalized transcript when request is valid")
        void shouldSaveSegment_whenValid() {
            // given
            CreateLessonSegmentRequest request = new CreateLessonSegmentRequest(
                    LESSON_ID, 1000, 60_000, "Don't worry, it's fine!", "Đừng lo, ổn mà!");
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(draftLesson));
            when(lessonSegmentRepository.existsByLessonIdAndStartMsLessThanAndEndMsGreaterThan(LESSON_ID, 60_000, 1000))
                    .thenReturn(false);
            when(lessonSegmentRepository.save(any(LessonSegment.class))).thenReturn(segment);
            when(lessonSegmentMapper.toAdminLessonSegmentResponse(segment)).thenReturn(segmentResponse);

            // when
            AdminLessonSegmentResponse result = lessonSegmentService.createSegment(request);

            // then
            assertThat(result).isEqualTo(segmentResponse);
            ArgumentCaptor<LessonSegment> captor = ArgumentCaptor.forClass(LessonSegment.class);
            verify(lessonSegmentRepository).save(captor.capture());
            LessonSegment saved = captor.getValue();
            assertThat(saved.getLesson()).isEqualTo(draftLesson);
            assertThat(saved.getStartMs()).isEqualTo(1000);
            assertThat(saved.getEndMs()).isEqualTo(60_000);
            assertThat(saved.getTranscriptOriginal()).isEqualTo("Don't worry, it's fine!");
            assertThat(saved.getTranscriptNormalized()).isEqualTo("dont worry its fine");
            assertThat(saved.getTranslation()).isEqualTo("Đừng lo, ổn mà!");
        }

        @Test
        @DisplayName("should allow creation when lesson durationMs is null")
        void shouldAllowCreate_whenLessonDurationIsNull() {
            // given
            draftLesson.setDurationMs(null);
            CreateLessonSegmentRequest request = new CreateLessonSegmentRequest(
                    LESSON_ID, 0, 999_999, "Hello", "Xin chào");
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(draftLesson));
            when(lessonSegmentRepository.existsByLessonIdAndStartMsLessThanAndEndMsGreaterThan(LESSON_ID, 999_999, 0))
                    .thenReturn(false);
            when(lessonSegmentRepository.save(any(LessonSegment.class))).thenReturn(segment);
            when(lessonSegmentMapper.toAdminLessonSegmentResponse(segment)).thenReturn(segmentResponse);

            // when
            AdminLessonSegmentResponse result = lessonSegmentService.createSegment(request);

            // then
            assertThat(result).isEqualTo(segmentResponse);
        }

        @Test
        @DisplayName("should throw LESSON_NOT_FOUND when lesson does not exist")
        void shouldThrowLessonNotFound_whenLessonMissing() {
            // given
            CreateLessonSegmentRequest request = new CreateLessonSegmentRequest(
                    LESSON_ID, 0, 5000, "Hello", "Xin chào");
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.createSegment(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.LESSON_NOT_FOUND));

            verify(lessonSegmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw INVALID_SEGMENT_TIME_RANGE when startMs >= endMs")
        void shouldThrowInvalidTimeRange_whenStartNotLessThanEnd() {
            // given
            CreateLessonSegmentRequest request = new CreateLessonSegmentRequest(
                    LESSON_ID, 5000, 5000, "Hello", "Xin chào");
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(draftLesson));

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.createSegment(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_SEGMENT_TIME_RANGE));

            verify(lessonSegmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw SEGMENT_EXCEEDS_DURATION when endMs > lesson durationMs")
        void shouldThrowExceedsDuration_whenEndMsExceedsDuration() {
            // given
            CreateLessonSegmentRequest request = new CreateLessonSegmentRequest(
                    LESSON_ID, 1000, 60_001, "Hello", "Xin chào");
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(draftLesson));

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.createSegment(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.SEGMENT_EXCEEDS_DURATION));

            verify(lessonSegmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw SEGMENT_TIME_OVERLAP when time range overlaps existing segment")
        void shouldThrowOverlap_whenTimeRangeOverlaps() {
            // given
            CreateLessonSegmentRequest request = new CreateLessonSegmentRequest(
                    LESSON_ID, 4000, 8000, "Hello", "Xin chào");
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(draftLesson));
            when(lessonSegmentRepository.existsByLessonIdAndStartMsLessThanAndEndMsGreaterThan(LESSON_ID, 8000, 4000))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.createSegment(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.SEGMENT_TIME_OVERLAP));

            verify(lessonSegmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw INVALID_REQUEST when normalized transcript is empty")
        void shouldThrowInvalidRequest_whenTranscriptHasOnlyPunctuation() {
            // given
            CreateLessonSegmentRequest request = new CreateLessonSegmentRequest(
                    LESSON_ID, 1000, 5000, "!!! ---", "...");
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(draftLesson));
            when(lessonSegmentRepository.existsByLessonIdAndStartMsLessThanAndEndMsGreaterThan(LESSON_ID, 5000, 1000))
                    .thenReturn(false);

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.createSegment(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));

            verify(lessonSegmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateSegment")
    class UpdateSegment {

        @Test
        @DisplayName("should update segment fields and regenerate normalized transcript")
        void shouldUpdateSegment_whenValid() {
            // given
            UpdateLessonSegmentRequest request = new UpdateLessonSegmentRequest(
                    2000, 7000, "Updated, text!", "Văn bản mới");
            when(lessonSegmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));
            when(lessonSegmentRepository.existsByLessonIdAndIdNotAndStartMsLessThanAndEndMsGreaterThan(
                    LESSON_ID, SEGMENT_ID, 7000, 2000)).thenReturn(false);
            when(lessonSegmentMapper.toAdminLessonSegmentResponse(segment)).thenReturn(segmentResponse);

            // when
            AdminLessonSegmentResponse result = lessonSegmentService.updateSegment(SEGMENT_ID, request);

            // then
            assertThat(result).isEqualTo(segmentResponse);
            assertThat(segment.getStartMs()).isEqualTo(2000);
            assertThat(segment.getEndMs()).isEqualTo(7000);
            assertThat(segment.getTranscriptOriginal()).isEqualTo("Updated, text!");
            assertThat(segment.getTranscriptNormalized()).isEqualTo("updated text");
            assertThat(segment.getTranslation()).isEqualTo("Văn bản mới");
        }

        @Test
        @DisplayName("should throw LESSON_SEGMENT_NOT_FOUND when segment does not exist")
        void shouldThrowSegmentNotFound_whenMissing() {
            // given
            UpdateLessonSegmentRequest request = new UpdateLessonSegmentRequest(
                    2000, 7000, "Updated", "Mới");
            when(lessonSegmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.updateSegment(SEGMENT_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_SEGMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw INVALID_SEGMENT_TIME_RANGE when startMs > endMs")
        void shouldThrowInvalidTimeRange_whenStartGreaterThanEnd() {
            // given
            UpdateLessonSegmentRequest request = new UpdateLessonSegmentRequest(
                    8000, 7000, "Updated", "Mới");
            when(lessonSegmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.updateSegment(SEGMENT_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_SEGMENT_TIME_RANGE));
        }

        @Test
        @DisplayName("should throw SEGMENT_EXCEEDS_DURATION when endMs exceeds lesson duration")
        void shouldThrowExceedsDuration_whenEndMsExceedsDuration() {
            // given
            UpdateLessonSegmentRequest request = new UpdateLessonSegmentRequest(
                    2000, 60_001, "Updated", "Mới");
            when(lessonSegmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.updateSegment(SEGMENT_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.SEGMENT_EXCEEDS_DURATION));
        }

        @Test
        @DisplayName("should throw SEGMENT_TIME_OVERLAP when overlapping another segment")
        void shouldThrowOverlap_whenOverlappingOtherSegment() {
            // given
            UpdateLessonSegmentRequest request = new UpdateLessonSegmentRequest(
                    2000, 7000, "Updated", "Mới");
            when(lessonSegmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));
            when(lessonSegmentRepository.existsByLessonIdAndIdNotAndStartMsLessThanAndEndMsGreaterThan(
                    LESSON_ID, SEGMENT_ID, 7000, 2000)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.updateSegment(SEGMENT_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.SEGMENT_TIME_OVERLAP));
        }

        @Test
        @DisplayName("should throw INVALID_REQUEST when normalized transcript is empty")
        void shouldThrowInvalidRequest_whenNormalizedEmpty() {
            // given
            UpdateLessonSegmentRequest request = new UpdateLessonSegmentRequest(
                    2000, 7000, "???", "Mới");
            when(lessonSegmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));
            when(lessonSegmentRepository.existsByLessonIdAndIdNotAndStartMsLessThanAndEndMsGreaterThan(
                    LESSON_ID, SEGMENT_ID, 7000, 2000)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.updateSegment(SEGMENT_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));
        }
    }

    @Nested
    @DisplayName("deleteSegments")
    class DeleteSegments {

        @Test
        @DisplayName("should delete all requested segments and progress in FK order for DRAFT lesson")
        void shouldDeleteAllSegments_whenDraftLesson() {
            // given
            LessonSegment seg2 = LessonSegment.builder().id(12L).lesson(draftLesson).build();
            DeleteLessonSegmentsRequest request = new DeleteLessonSegmentsRequest(Set.of(11L, 12L));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(draftLesson));
            when(lessonSegmentRepository.findAllByIdInAndLessonId(request.segmentIds(), LESSON_ID))
                    .thenReturn(List.of(segment, seg2));

            // when
            lessonSegmentService.deleteSegments(LESSON_ID, request);

            // then
            verify(segmentWordAccuracyRepository).deleteAllBySegmentIds(List.of(11L, 12L));
            verify(userSegmentProgressRepository).deleteAllBySegmentIds(List.of(11L, 12L));
            verify(lessonSegmentRepository).deleteAll(List.of(segment, seg2));
        }

        @Test
        @DisplayName("should delete segment when PUBLISHED lesson retains at least one segment")
        void shouldDeleteSegment_whenPublishedLessonKeepsAtLeastOne() {
            // given
            DeleteLessonSegmentsRequest request = new DeleteLessonSegmentsRequest(Set.of(11L));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(publishedLesson));
            when(lessonSegmentRepository.findAllByIdInAndLessonId(request.segmentIds(), LESSON_ID))
                    .thenReturn(List.of(segment));
            when(lessonSegmentRepository.countByLessonId(LESSON_ID)).thenReturn(2L);

            // when
            lessonSegmentService.deleteSegments(LESSON_ID, request);

            // then
            verify(segmentWordAccuracyRepository).deleteAllBySegmentIds(List.of(11L));
            verify(userSegmentProgressRepository).deleteAllBySegmentIds(List.of(11L));
            verify(lessonSegmentRepository).deleteAll(List.of(segment));
        }

        @Test
        @DisplayName("should throw LESSON_NOT_FOUND when lesson does not exist")
        void shouldThrowLessonNotFound_whenLessonMissing() {
            // given
            DeleteLessonSegmentsRequest request = new DeleteLessonSegmentsRequest(Set.of(11L));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.deleteSegments(LESSON_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.LESSON_NOT_FOUND));

            verifyNoInteractions(segmentWordAccuracyRepository, userSegmentProgressRepository);
        }

        @Test
        @DisplayName("should throw LESSON_SEGMENT_NOT_FOUND when any segmentId does not belong to lesson")
        void shouldThrowSegmentNotFound_whenAnySegmentMissingFromLesson() {
            // given
            DeleteLessonSegmentsRequest request = new DeleteLessonSegmentsRequest(Set.of(11L, 999L));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(draftLesson));
            when(lessonSegmentRepository.findAllByIdInAndLessonId(request.segmentIds(), LESSON_ID))
                    .thenReturn(List.of(segment));

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.deleteSegments(LESSON_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_SEGMENT_NOT_FOUND));

            verifyNoInteractions(segmentWordAccuracyRepository, userSegmentProgressRepository);
            verify(lessonSegmentRepository, never()).deleteAll(any());
        }

        @Test
        @DisplayName("should throw LESSON_SEGMENT_REQUIRED when deleting last segment of PUBLISHED lesson")
        void shouldThrowLessonSegmentRequired_whenRemovingAllFromPublished() {
            // given
            DeleteLessonSegmentsRequest request = new DeleteLessonSegmentsRequest(Set.of(11L));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(publishedLesson));
            when(lessonSegmentRepository.findAllByIdInAndLessonId(request.segmentIds(), LESSON_ID))
                    .thenReturn(List.of(segment));
            when(lessonSegmentRepository.countByLessonId(LESSON_ID)).thenReturn(1L);

            // when & then
            assertThatThrownBy(() -> lessonSegmentService.deleteSegments(LESSON_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_SEGMENT_REQUIRED));

            verifyNoInteractions(segmentWordAccuracyRepository, userSegmentProgressRepository);
            verify(lessonSegmentRepository, never()).deleteAll(any());
        }
    }
}
