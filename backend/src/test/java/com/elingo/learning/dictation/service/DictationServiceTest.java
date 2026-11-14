package com.elingo.learning.dictation.service;

import com.elingo.common.event.XpGainedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.entity.XpSource;
import com.elingo.learning.dictation.dto.request.UpdateDictationProgressRequest;
import com.elingo.learning.dictation.service.impl.DictationServiceImpl;
import com.elingo.learning.lesson.config.LessonProperties;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.dto.response.SegmentSubmitResponse;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.learning.lesson.entity.ProgressStatus;
import com.elingo.learning.lesson.entity.UserLessonProgress;
import com.elingo.learning.lesson.entity.UserSegmentProgress;
import com.elingo.learning.lesson.enums.LessonMode;
import com.elingo.learning.lesson.mapper.LessonProgressMapper;
import com.elingo.learning.lesson.repository.LessonSegmentRepository;
import com.elingo.learning.lesson.service.LessonAccessService;
import com.elingo.learning.lesson.service.LessonProgressService;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class DictationServiceTest {

    @Mock
    private LessonAccessService lessonAccessService;

    @Mock
    private LessonSegmentRepository lessonSegmentRepository;

    @Mock
    private LessonProgressService lessonProgressService;

    @Mock
    private DictationScoringService scoringService;

    @Mock
    private LessonProgressMapper lessonProgressMapper;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private DictationServiceImpl dictationService;

    private static final Long USER_ID = 1L;
    private static final Long LESSON_ID = 10L;
    private static final Long SEGMENT_ID = 100L;
    private static final Integer SEGMENT_START_MS = 3000;
    private static final LessonProperties PROPS = new LessonProperties(60, 55, 5, 10);

    private User testUser;
    private Lesson testLesson;
    private LessonSegment testSegment;
    private UserSegmentProgress progress;
    private UserLessonProgress lessonProgress;
    private SegmentProgressResponse segmentResponse;
    private LessonProgressSummaryResponse summaryResponse;

    @BeforeEach
    void setUp() {
        // LessonProperties là record nên không @Mock được -> gán trực tiếp
        ReflectionTestUtils.setField(dictationService, "lessonProperties", PROPS);

        testUser = User.builder().id(USER_ID).username("user1").fullName("User One").role(Role.USER).build();
        testLesson = Lesson.builder()
                .id(LESSON_ID).title("Lesson").slug("lesson").status(LessonStatus.PUBLISHED)
                .sourceUrl("https://www.youtube.com/watch?v=abc").build();
        // 4 từ -> totalWords = 4
        testSegment = LessonSegment.builder()
                .id(SEGMENT_ID).lesson(testLesson).startMs(SEGMENT_START_MS).endMs(6000)
                .transcriptOriginal("Hello, big wide world!").transcriptNormalized("hello big wide world").build();
        progress = UserSegmentProgress.builder()
                .id(500L).user(testUser).lesson(testLesson).segment(testSegment).build();
        lessonProgress = UserLessonProgress.builder().id(900L).user(testUser).lesson(testLesson).build();
        segmentResponse = new SegmentProgressResponse(SEGMENT_ID, 1, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, null, List.of());
        summaryResponse = new LessonProgressSummaryResponse(
                ProgressStatus.IN_PROGRESS, new BigDecimal("25.00"), SEGMENT_START_MS,
                ProgressStatus.NOT_STARTED, BigDecimal.ZERO, null);
    }

    /** Stub các bước chung trước khi tính điểm */
    private void stubAccessAndSegment() {
        when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID)).thenReturn(testLesson);
        when(lessonSegmentRepository.findByIdAndLessonId(SEGMENT_ID, LESSON_ID)).thenReturn(Optional.of(testSegment));
    }

    /** Stub toàn bộ luồng thành công với điểm tính ra là {@code score} */
    private void stubSuccessFlow(int attemptCount, int expectedHint, int score) {
        stubAccessAndSegment();
        when(lessonProgressService.getOrCreateSegmentProgress(USER_ID, testSegment)).thenReturn(progress);
        when(scoringService.calculateScore(attemptCount, expectedHint, 4)).thenReturn(score);
        when(lessonProgressService.recordSegmentSubmitted(USER_ID, LESSON_ID, LessonMode.DICTATION, SEGMENT_START_MS))
                .thenReturn(lessonProgress);
        when(lessonProgressMapper.toSegmentProgressResponse(progress, List.of())).thenReturn(segmentResponse);
        when(lessonProgressMapper.toLessonProgressSummaryResponse(lessonProgress)).thenReturn(summaryResponse);
    }

    // ========================================================================
    // updateProgress
    // ========================================================================
    @Nested
    @DisplayName("updateProgress")
    class UpdateProgressTests {

        @Test
        @DisplayName("Update progress successfully on the first attempt and pass the segment")
        void updateProgress_Success_FirstAttemptPassed() {
            stubSuccessFlow(1, 1, 85);

            SegmentSubmitResponse response = dictationService.updateProgress(
                    USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(1, 1));

            assertThat(response.score()).isEqualTo(85);
            assertThat(response.passed()).isTrue();
            assertThat(response.segmentProgress()).isSameAs(segmentResponse);
            assertThat(response.lessonProgress()).isSameAs(summaryResponse);

            assertThat(progress.getDictationAttemptCount()).isEqualTo(1);
            assertThat(progress.getDictationBestScore()).isEqualByComparingTo("85.00");
            assertThat(progress.getDictationHintUsedCount()).isEqualTo(1);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(publisher).publishEvent(eventCaptor.capture());
            XpGainedEvent event = (XpGainedEvent) eventCaptor.getValue();
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.refId()).isEqualTo(SEGMENT_ID);
            assertThat(event.xpSource()).isEqualTo(XpSource.SEGMENT_DICTATION);
        }

        @Test
        @DisplayName("Update progress successfully when the score is below the pass score")
        void updateProgress_Success_NotPassed() {
            stubSuccessFlow(3, 2, 40);

            SegmentSubmitResponse response = dictationService.updateProgress(
                    USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(3, 2));

            assertThat(response.score()).isEqualTo(40);
            assertThat(response.passed()).isFalse();
            assertThat(progress.getDictationAttemptCount()).isEqualTo(3);
            assertThat(progress.getDictationBestScore()).isEqualByComparingTo("40.00");
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Update progress records the first attempt even when the score is zero")
        void updateProgress_Success_FirstAttemptZeroScore() {
            stubSuccessFlow(1, 4, 0);

            SegmentSubmitResponse response = dictationService.updateProgress(
                    USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(1, 4));

            assertThat(response.score()).isZero();
            assertThat(response.passed()).isFalse();
            assertThat(progress.getDictationBestScore()).isEqualByComparingTo("0.00");
            // Lần nộp đầu tiên luôn ghi hint dù điểm không lớn hơn best cũ (0)
            assertThat(progress.getDictationHintUsedCount()).isEqualTo(4);
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Update progress clamps hint used count to the number of words")
        void updateProgress_Success_HintClampedToTotalWords() {
            stubSuccessFlow(1, 4, 0);

            dictationService.updateProgress(
                    USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(1, 50));

            verify(scoringService).calculateScore(1, 4, 4);
            assertThat(progress.getDictationHintUsedCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("Update progress keeps best score and hint count when a later attempt scores lower")
        void updateProgress_Success_LowerScoreKeepsBest() {
            progress.setDictationAttemptCount(1);
            progress.setDictationBestScore(new BigDecimal("80.00"));
            progress.setDictationHintUsedCount(1);
            stubSuccessFlow(2, 0, 70);

            SegmentSubmitResponse response = dictationService.updateProgress(
                    USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(2, 0));

            assertThat(response.score()).isEqualTo(70);
            assertThat(response.passed()).isTrue();
            assertThat(progress.getDictationAttemptCount()).isEqualTo(2);
            assertThat(progress.getDictationBestScore()).isEqualByComparingTo("80.00");
            assertThat(progress.getDictationHintUsedCount()).isEqualTo(1);
            // Đã đạt ngưỡng từ trước nên không phát XP lần nữa
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Update progress updates best score and hint count when a later attempt scores higher")
        void updateProgress_Success_HigherScoreUpdatesBest() {
            progress.setDictationAttemptCount(1);
            progress.setDictationBestScore(new BigDecimal("40.00"));
            progress.setDictationHintUsedCount(3);
            stubSuccessFlow(2, 0, 75);

            SegmentSubmitResponse response = dictationService.updateProgress(
                    USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(2, 0));

            assertThat(response.score()).isEqualTo(75);
            assertThat(progress.getDictationBestScore()).isEqualByComparingTo("75.00");
            assertThat(progress.getDictationHintUsedCount()).isZero();
            // Lần đầu vượt ngưỡng -> phát XP
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(publisher).publishEvent(eventCaptor.capture());
            assertThat(((XpGainedEvent) eventCaptor.getValue()).xpSource()).isEqualTo(XpSource.SEGMENT_DICTATION);
        }

        @Test
        @DisplayName("Update progress does not decrease the stored attempt count")
        void updateProgress_Success_AttemptCountIsMax() {
            progress.setDictationAttemptCount(5);
            progress.setDictationBestScore(new BigDecimal("50.00"));
            stubSuccessFlow(2, 0, 30);

            dictationService.updateProgress(
                    USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(2, 0));

            assertThat(progress.getDictationAttemptCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Update progress passes exactly at the pass score and awards XP once")
        void updateProgress_Success_ExactlyPassScore() {
            progress.setDictationAttemptCount(1);
            progress.setDictationBestScore(new BigDecimal("59.00"));
            stubSuccessFlow(2, 0, PROPS.dictationPassScore());

            SegmentSubmitResponse response = dictationService.updateProgress(
                    USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(2, 0));

            assertThat(response.passed()).isTrue();
            verify(publisher).publishEvent(any(XpGainedEvent.class));
        }

        @Test
        @DisplayName("Update progress does not award XP when previous best already reached the pass score")
        void updateProgress_Success_NoDuplicateXp() {
            progress.setDictationAttemptCount(1);
            progress.setDictationBestScore(new BigDecimal("60.00"));
            stubSuccessFlow(2, 0, 90);

            dictationService.updateProgress(
                    USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(2, 0));

            verify(publisher, never()).publishEvent(any(XpGainedEvent.class));
        }

        @Test
        @DisplayName("Update progress records the submit with the segment start time")
        void updateProgress_Success_RecordsLessonProgress() {
            stubSuccessFlow(1, 0, 90);

            dictationService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(1, 0));

            verify(lessonProgressService).recordSegmentSubmitted(USER_ID, LESSON_ID, LessonMode.DICTATION, SEGMENT_START_MS);
            verify(lessonProgressMapper).toSegmentProgressResponse(progress, List.of());
            verify(lessonProgressMapper).toLessonProgressSummaryResponse(lessonProgress);
        }

        @Test
        @DisplayName("Update progress failed: Lesson not found")
        void updateProgress_Fail_LessonNotFound() {
            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            assertThatThrownBy(() -> dictationService.updateProgress(
                    USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(1, 0)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_NOT_FOUND));

            verifyNoInteractions(lessonSegmentRepository);
            verifyNoInteractions(lessonProgressService);
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Update progress failed: Premium required")
        void updateProgress_Fail_PremiumRequired() {
            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID))
                    .thenThrow(new AppException(AppError.PREMIUM_REQUIRED));

            assertThatThrownBy(() -> dictationService.updateProgress(
                    USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(1, 0)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PREMIUM_REQUIRED));

            verifyNoInteractions(lessonProgressService);
        }

        @Test
        @DisplayName("Update progress failed: Segment not found in the lesson")
        void updateProgress_Fail_SegmentNotFound() {
            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID)).thenReturn(testLesson);
            when(lessonSegmentRepository.findByIdAndLessonId(SEGMENT_ID, LESSON_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dictationService.updateProgress(
                    USER_ID, LESSON_ID, SEGMENT_ID, new UpdateDictationProgressRequest(1, 0)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_SEGMENT_NOT_FOUND));

            verifyNoInteractions(lessonProgressService);
            verify(scoringService, never()).calculateScore(anyInt(), anyInt(), anyInt());
            verifyNoInteractions(publisher);
        }
    }
}
