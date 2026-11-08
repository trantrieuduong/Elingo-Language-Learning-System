package com.elingo.learning.shadowing.service;

import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.event.XpGainedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.entity.XpSource;
import com.elingo.learning.lesson.config.LessonProperties;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.dto.response.SegmentSubmitResponse;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.learning.lesson.entity.ProgressStatus;
import com.elingo.learning.lesson.entity.SegmentWordAccuracy;
import com.elingo.learning.lesson.entity.UserLessonProgress;
import com.elingo.learning.lesson.entity.UserSegmentProgress;
import com.elingo.learning.lesson.enums.LessonMode;
import com.elingo.learning.lesson.mapper.LessonProgressMapper;
import com.elingo.learning.lesson.repository.LessonSegmentRepository;
import com.elingo.learning.lesson.service.LessonAccessService;
import com.elingo.learning.lesson.service.LessonProgressService;
import com.elingo.learning.shadowing.dto.request.UpdateShadowingProgressRequest;
import com.elingo.learning.shadowing.dto.request.WordAccuracyItem;
import com.elingo.learning.shadowing.service.impl.ShadowingServiceImpl;
import com.elingo.progress.service.ProgressService;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ShadowingServiceTest {

    @Mock
    private LessonAccessService lessonAccessService;

    @Mock
    private LessonSegmentRepository lessonSegmentRepository;

    @Mock
    private LessonProgressService lessonProgressService;

    @Mock
    private LessonProgressMapper lessonProgressMapper;

    @Mock
    private ProgressService progressService;

    @Mock
    private ShadowingScoringService scoringService;

    @Mock
    private ApplicationEventPublisher publisher;

    @Captor
    private ArgumentCaptor<List<SegmentWordAccuracy>> wordsCaptor;

    @InjectMocks
    private ShadowingServiceImpl shadowingService;

    private static final Long USER_ID = 1L;
    private static final Long LESSON_ID = 10L;
    private static final Long SEGMENT_ID = 100L;
    private static final Integer SEGMENT_START_MS = 3000;
    private static final String AUDIO_KEY = "verified/1/recording.mp3";
    private static final String NEW_UPLOADS_KEY = "uploads/1/recording.mp3";
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
        ReflectionTestUtils.setField(shadowingService, "lessonProperties", PROPS);

        testUser = User.builder().id(USER_ID).username("user1").fullName("User One").role(Role.USER).build();
        testLesson = Lesson.builder()
                .id(LESSON_ID).title("Lesson").slug("lesson").status(LessonStatus.PUBLISHED)
                .sourceUrl("https://www.youtube.com/watch?v=abc").build();
        // 3 từ -> giới hạn wordsAccuracy = max(3 * 2, 20) = 20
        testSegment = LessonSegment.builder()
                .id(SEGMENT_ID).lesson(testLesson).startMs(SEGMENT_START_MS).endMs(6000)
                .transcriptOriginal("Hello, big world!").transcriptNormalized("hello big world").build();
        progress = UserSegmentProgress.builder()
                .id(500L).user(testUser).lesson(testLesson).segment(testSegment).build();
        lessonProgress = UserLessonProgress.builder().id(900L).user(testUser).lesson(testLesson).build();
        segmentResponse = new SegmentProgressResponse(SEGMENT_ID, 0, BigDecimal.ZERO, 0, 1, BigDecimal.ZERO, null, List.of());
        summaryResponse = new LessonProgressSummaryResponse(
                ProgressStatus.NOT_STARTED, BigDecimal.ZERO, null,
                ProgressStatus.IN_PROGRESS, new BigDecimal("25.00"), SEGMENT_START_MS);
    }

    private List<WordAccuracyItem> words() {
        return List.of(
                new WordAccuracyItem("hello", new BigDecimal("95.5")),
                new WordAccuracyItem("world", new BigDecimal("60")));
    }

    private UpdateShadowingProgressRequest request(int attemptCount, int score) {
        return new UpdateShadowingProgressRequest(attemptCount, score, words(), AUDIO_KEY);
    }

    private void stubAccessAndSegment() {
        when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID)).thenReturn(testLesson);
        when(lessonSegmentRepository.findByIdAndLessonId(SEGMENT_ID, LESSON_ID)).thenReturn(Optional.of(testSegment));
    }

    /** Stub toàn bộ luồng thành công, điểm cuối cùng sau khi trừ penalty là {@code finalScore} */
    private void stubSuccessFlow(UpdateShadowingProgressRequest request, int finalScore) {
        stubAccessAndSegment();
        when(scoringService.calculateFinalScore(request.score(), request.attemptCount())).thenReturn(finalScore);
        when(lessonProgressService.getOrCreateSegmentProgress(USER_ID, testSegment)).thenReturn(progress);
        when(lessonProgressService.recordSegmentSubmitted(USER_ID, LESSON_ID, LessonMode.SHADOWING, SEGMENT_START_MS))
                .thenReturn(lessonProgress);
        when(lessonProgressMapper.toLessonProgressSummaryResponse(lessonProgress)).thenReturn(summaryResponse);
        when(progressService.buildSegmentProgress(progress)).thenReturn(segmentResponse);
    }

    private List<Object> capturePublishedEvents(int expectedCount) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher, times(expectedCount)).publishEvent(captor.capture());
        return new ArrayList<>(captor.getAllValues());
    }

    private void assertNothingWritten() {
        verifyNoInteractions(lessonProgressService);
        verifyNoInteractions(publisher);
        verifyNoInteractions(progressService);
    }

    // ========================================================================
    // updateProgress - success
    // ========================================================================
    @Nested
    @DisplayName("updateProgress success")
    class UpdateProgressSuccessTests {

        @Test
        @DisplayName("Update progress successfully on the first attempt and pass the segment")
        void updateProgress_Success_FirstAttemptPassed() {
            UpdateShadowingProgressRequest request = request(1, 80);
            stubSuccessFlow(request, 80);

            SegmentSubmitResponse response = shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            assertThat(response.score()).isEqualTo(80);
            assertThat(response.passed()).isTrue();
            assertThat(response.segmentProgress()).isSameAs(segmentResponse);
            assertThat(response.lessonProgress()).isSameAs(summaryResponse);

            assertThat(progress.getShadowingAttemptCount()).isEqualTo(1);
            assertThat(progress.getShadowingBestScore()).isEqualByComparingTo("80.00");
            assertThat(progress.getShadowingLatestAudioUrl()).isEqualTo(NEW_UPLOADS_KEY);

            // Có file mới và XP cho segment; không có file cũ để xóa
            List<Object> events = capturePublishedEvents(2);
            assertThat(events).anySatisfy(e -> assertThat(e).isEqualTo(new FileAttachedEvent(AUDIO_KEY, NEW_UPLOADS_KEY)));
            assertThat(events).anySatisfy(e -> {
                assertThat(e).isInstanceOf(XpGainedEvent.class);
                XpGainedEvent xp = (XpGainedEvent) e;
                assertThat(xp.userId()).isEqualTo(USER_ID);
                assertThat(xp.refId()).isEqualTo(SEGMENT_ID);
                assertThat(xp.xpSource()).isEqualTo(XpSource.SEGMENT_SHADOWING);
            });
            assertThat(events).noneMatch(e -> e instanceof FileDeletedEvent);
        }

        @Test
        @DisplayName("Update progress saves trimmed words with accuracy rounded to two decimals")
        void updateProgress_Success_ReplacesWordsAccuracy() {
            List<WordAccuracyItem> items = List.of(
                    new WordAccuracyItem("  hello ", new BigDecimal("95.456")),
                    new WordAccuracyItem("world", new BigDecimal("60")));
            UpdateShadowingProgressRequest request = new UpdateShadowingProgressRequest(1, 80, items, AUDIO_KEY);
            stubSuccessFlow(request, 80);

            shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            verify(lessonProgressService).replaceWordsAccuracy(eq(progress), wordsCaptor.capture());
            List<SegmentWordAccuracy> saved = wordsCaptor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved.get(0).getWord()).isEqualTo("hello");
            assertThat(saved.get(0).getAccuracy()).isEqualByComparingTo("95.46");
            assertThat(saved.get(0).getAccuracy().scale()).isEqualTo(2);
            assertThat(saved.get(0).getUserSegmentProgress()).isSameAs(progress);
            assertThat(saved.get(1).getWord()).isEqualTo("world");
            assertThat(saved.get(1).getAccuracy()).isEqualByComparingTo("60.00");
        }

        @Test
        @DisplayName("Update progress successfully with an empty words accuracy list")
        void updateProgress_Success_EmptyWordsAccuracy() {
            UpdateShadowingProgressRequest request = new UpdateShadowingProgressRequest(1, 80, List.of(), AUDIO_KEY);
            stubSuccessFlow(request, 80);

            shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            verify(lessonProgressService).replaceWordsAccuracy(eq(progress), wordsCaptor.capture());
            assertThat(wordsCaptor.getValue()).isEmpty();
        }

        @Test
        @DisplayName("Update progress successfully accepts exactly the maximum number of words")
        void updateProgress_Success_MaxWordsAccuracy() {
            List<WordAccuracyItem> items = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                items.add(new WordAccuracyItem("w" + i, BigDecimal.TEN));
            }
            UpdateShadowingProgressRequest request = new UpdateShadowingProgressRequest(1, 80, items, AUDIO_KEY);
            stubSuccessFlow(request, 80);

            SegmentSubmitResponse response = shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            assertThat(response.score()).isEqualTo(80);
        }

        @Test
        @DisplayName("Update progress successfully allows more words when the transcript is long")
        void updateProgress_Success_LongTranscriptAllowsMoreWords() {
            // 15 từ -> giới hạn = max(30, 20) = 30
            testSegment.setTranscriptNormalized("one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen");
            List<WordAccuracyItem> items = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                items.add(new WordAccuracyItem("w" + i, BigDecimal.TEN));
            }
            UpdateShadowingProgressRequest request = new UpdateShadowingProgressRequest(1, 80, items, AUDIO_KEY);
            stubSuccessFlow(request, 80);

            SegmentSubmitResponse response = shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            assertThat(response.score()).isEqualTo(80);
        }

        @Test
        @DisplayName("Update progress successfully but not passed when final score is below the pass score")
        void updateProgress_Success_NotPassed() {
            UpdateShadowingProgressRequest request = request(3, 70);
            stubSuccessFlow(request, 40);

            SegmentSubmitResponse response = shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            assertThat(response.score()).isEqualTo(40);
            assertThat(response.passed()).isFalse();
            assertThat(progress.getShadowingAttemptCount()).isEqualTo(3);
            assertThat(progress.getShadowingBestScore()).isEqualByComparingTo("40.00");
            // Không đạt nên chỉ có event gắn file, không có XP
            List<Object> events = capturePublishedEvents(1);
            assertThat(events.get(0)).isInstanceOf(FileAttachedEvent.class);
        }

        @Test
        @DisplayName("Update progress passes exactly at the pass score")
        void updateProgress_Success_ExactlyPassScore() {
            UpdateShadowingProgressRequest request = request(1, PROPS.shadowingPassScore());
            stubSuccessFlow(request, PROPS.shadowingPassScore());

            SegmentSubmitResponse response = shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            assertThat(response.passed()).isTrue();
            verify(publisher).publishEvent(any(XpGainedEvent.class));
        }

        @Test
        @DisplayName("Update progress keeps the best score when a later attempt scores lower")
        void updateProgress_Success_LowerScoreKeepsBest() {
            progress.setShadowingAttemptCount(1);
            progress.setShadowingBestScore(new BigDecimal("90.00"));
            progress.setShadowingLatestAudioUrl("uploads/1/previous.mp3");
            UpdateShadowingProgressRequest request = request(2, 80);
            stubSuccessFlow(request, 70);

            SegmentSubmitResponse response = shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            assertThat(response.score()).isEqualTo(70);
            assertThat(response.passed()).isTrue();
            assertThat(progress.getShadowingBestScore()).isEqualByComparingTo("90.00");
            assertThat(progress.getShadowingAttemptCount()).isEqualTo(2);
            // Đã đạt ngưỡng từ trước nên không phát XP lần nữa
            verify(publisher, never()).publishEvent(any(XpGainedEvent.class));
        }

        @Test
        @DisplayName("Update progress updates the best score when a later attempt scores higher")
        void updateProgress_Success_HigherScoreUpdatesBest() {
            progress.setShadowingAttemptCount(1);
            progress.setShadowingBestScore(new BigDecimal("40.00"));
            UpdateShadowingProgressRequest request = request(2, 90);
            stubSuccessFlow(request, 80);

            shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            assertThat(progress.getShadowingBestScore()).isEqualByComparingTo("80.00");
            // Lần đầu vượt ngưỡng -> phát XP
            verify(publisher).publishEvent(any(XpGainedEvent.class));
        }

        @Test
        @DisplayName("Update progress does not decrease the stored attempt count")
        void updateProgress_Success_AttemptCountIsMax() {
            progress.setShadowingAttemptCount(5);
            progress.setShadowingBestScore(new BigDecimal("50.00"));
            UpdateShadowingProgressRequest request = request(2, 60);
            stubSuccessFlow(request, 50);

            shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            assertThat(progress.getShadowingAttemptCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Update progress deletes the previous recording in uploads when a new one is submitted")
        void updateProgress_Success_ReplacesOldAudio() {
            progress.setShadowingLatestAudioUrl("uploads/1/old.mp3");
            UpdateShadowingProgressRequest request = request(1, 80);
            stubSuccessFlow(request, 80);

            shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            assertThat(progress.getShadowingLatestAudioUrl()).isEqualTo(NEW_UPLOADS_KEY);
            List<Object> events = capturePublishedEvents(3);
            assertThat(events).anySatisfy(e -> assertThat(e).isEqualTo(new FileAttachedEvent(AUDIO_KEY, NEW_UPLOADS_KEY)));
            assertThat(events).anySatisfy(e -> assertThat(e).isEqualTo(new FileDeletedEvent(List.of("uploads/1/old.mp3"))));
        }

        @Test
        @DisplayName("Update progress does not delete an old recording that is outside the uploads zone")
        void updateProgress_Success_OldAudioOutsideUploadsNotDeleted() {
            progress.setShadowingLatestAudioUrl("verified/1/legacy.mp3");
            UpdateShadowingProgressRequest request = request(1, 80);
            stubSuccessFlow(request, 80);

            shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            List<Object> events = capturePublishedEvents(2);
            assertThat(events).noneMatch(e -> e instanceof FileDeletedEvent);
        }

        @Test
        @DisplayName("Update progress does not publish file events when the recording is unchanged")
        void updateProgress_Success_SameAudioNoFileEvents() {
            progress.setShadowingLatestAudioUrl(NEW_UPLOADS_KEY);
            UpdateShadowingProgressRequest request = request(1, 80);
            stubSuccessFlow(request, 80);

            shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            assertThat(progress.getShadowingLatestAudioUrl()).isEqualTo(NEW_UPLOADS_KEY);
            // Chỉ còn event XP
            List<Object> events = capturePublishedEvents(1);
            assertThat(events.get(0)).isInstanceOf(XpGainedEvent.class);
        }

        @Test
        @DisplayName("Update progress records the submit with the segment start time")
        void updateProgress_Success_RecordsLessonProgress() {
            UpdateShadowingProgressRequest request = request(1, 80);
            stubSuccessFlow(request, 80);

            shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);

            verify(lessonProgressService).recordSegmentSubmitted(USER_ID, LESSON_ID, LessonMode.SHADOWING, SEGMENT_START_MS);
            verify(progressService).buildSegmentProgress(progress);
            verify(lessonProgressMapper).toLessonProgressSummaryResponse(lessonProgress);
        }
    }

    // ========================================================================
    // updateProgress - failure
    // ========================================================================
    @Nested
    @DisplayName("updateProgress failure")
    class UpdateProgressFailureTests {

        @Test
        @DisplayName("Update progress failed: Lesson not found")
        void updateProgress_Fail_LessonNotFound() {
            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            assertThatThrownBy(() -> shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request(1, 80)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_NOT_FOUND));

            verifyNoInteractions(lessonSegmentRepository);
            assertNothingWritten();
        }

        @Test
        @DisplayName("Update progress failed: Premium required")
        void updateProgress_Fail_PremiumRequired() {
            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID))
                    .thenThrow(new AppException(AppError.PREMIUM_REQUIRED));

            assertThatThrownBy(() -> shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request(1, 80)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PREMIUM_REQUIRED));

            assertNothingWritten();
        }

        @Test
        @DisplayName("Update progress failed: Segment not found in the lesson")
        void updateProgress_Fail_SegmentNotFound() {
            when(lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID)).thenReturn(testLesson);
            when(lessonSegmentRepository.findByIdAndLessonId(SEGMENT_ID, LESSON_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request(1, 80)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_SEGMENT_NOT_FOUND));

            assertNothingWritten();
        }

        @Test
        @DisplayName("Update progress failed: Too many words accuracy items for the transcript")
        void updateProgress_Fail_TooManyWords() {
            stubAccessAndSegment();
            List<WordAccuracyItem> items = new ArrayList<>();
            for (int i = 0; i < 21; i++) {
                items.add(new WordAccuracyItem("w" + i, BigDecimal.TEN));
            }
            UpdateShadowingProgressRequest request = new UpdateShadowingProgressRequest(1, 80, items, AUDIO_KEY);

            assertThatThrownBy(() -> shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.WORDS_ACCURACY_INVALID));

            verify(scoringService, never()).calculateFinalScore(anyInt(), anyInt());
            assertNothingWritten();
        }

        @Test
        @DisplayName("Update progress failed: Audio belongs to another user")
        void updateProgress_Fail_AudioOfAnotherUser() {
            stubAccessAndSegment();
            UpdateShadowingProgressRequest request =
                    new UpdateShadowingProgressRequest(1, 80, words(), "verified/2/recording.mp3");

            assertThatThrownBy(() -> shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_ACCESS_DENIED));

            verify(scoringService, never()).calculateFinalScore(anyInt(), anyInt());
            assertNothingWritten();
        }

        @Test
        @DisplayName("Update progress failed: Audio key tries path traversal")
        void updateProgress_Fail_AudioPathTraversal() {
            stubAccessAndSegment();
            UpdateShadowingProgressRequest request =
                    new UpdateShadowingProgressRequest(1, 80, words(), "verified/1/../2/recording.mp3");

            assertThatThrownBy(() -> shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_ACCESS_DENIED));

            assertNothingWritten();
        }

        @Test
        @DisplayName("Update progress failed: Audio is not in the verified zone")
        void updateProgress_Fail_AudioNotVerified() {
            stubAccessAndSegment();
            UpdateShadowingProgressRequest request =
                    new UpdateShadowingProgressRequest(1, 80, words(), "staging/1/recording.mp3");

            assertThatThrownBy(() -> shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_REQUEST));

            verify(scoringService, never()).calculateFinalScore(anyInt(), anyInt());
            assertNothingWritten();
        }

        @Test
        @DisplayName("Update progress failed: Audio is already in the uploads zone")
        void updateProgress_Fail_AudioAlreadyUploaded() {
            stubAccessAndSegment();
            UpdateShadowingProgressRequest request =
                    new UpdateShadowingProgressRequest(1, 80, words(), NEW_UPLOADS_KEY);

            assertThatThrownBy(() -> shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_REQUEST));

            assertNothingWritten();
        }
    }
}
