package com.elingo.learning.lesson.service;

import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.learning.lesson.entity.ProgressStatus;
import com.elingo.learning.lesson.entity.SegmentWordAccuracy;
import com.elingo.learning.lesson.entity.UserLessonProgress;
import com.elingo.learning.lesson.entity.UserSegmentProgress;
import com.elingo.learning.lesson.enums.LessonMode;
import com.elingo.learning.lesson.repository.LessonRepository;
import com.elingo.learning.lesson.repository.LessonSegmentRepository;
import com.elingo.learning.lesson.repository.SegmentWordAccuracyRepository;
import com.elingo.learning.lesson.repository.UserLessonProgressRepository;
import com.elingo.learning.lesson.repository.UserSegmentProgressRepository;
import com.elingo.learning.lesson.service.impl.LessonProgressServiceImpl;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LessonProgressServiceTest {

    @Mock
    private UserLessonProgressRepository userLessonProgressRepository;

    @Mock
    private UserSegmentProgressRepository userSegmentProgressRepository;

    @Mock
    private SegmentWordAccuracyRepository segmentWordAccuracyRepository;

    @Mock
    private LessonSegmentRepository lessonSegmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private LessonProgressServiceImpl lessonProgressService;

    private static final Long USER_ID = 1L;
    private static final Long LESSON_ID = 10L;
    private static final Long SEGMENT_ID = 100L;
    private static final Integer SEGMENT_START_MS = 3000;

    private User testUser;
    private Lesson testLesson;
    private LessonSegment testSegment;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(USER_ID).username("user1").fullName("User One").role(Role.USER).build();
        testLesson = Lesson.builder()
                .id(LESSON_ID)
                .title("Lesson")
                .slug("lesson")
                .status(LessonStatus.PUBLISHED)
                .sourceUrl("https://www.youtube.com/watch?v=abc")
                .build();
        testSegment = LessonSegment.builder()
                .id(SEGMENT_ID)
                .lesson(testLesson)
                .startMs(SEGMENT_START_MS)
                .endMs(6000)
                .transcriptOriginal("Hello, world!")
                .transcriptNormalized("hello world")
                .build();
    }

    // ========================================================================
    // getOrCreateSegmentProgress
    // ========================================================================
    @Nested
    @DisplayName("getOrCreateSegmentProgress")
    class GetOrCreateSegmentProgressTests {

        @Test
        @DisplayName("Get existing segment progress without creating a new one")
        void getOrCreateSegmentProgress_Success_Existing() {
            UserSegmentProgress existing = UserSegmentProgress.builder()
                    .id(500L).user(testUser).lesson(testLesson).segment(testSegment).build();
            when(userSegmentProgressRepository.findByUserIdAndSegmentId(USER_ID, SEGMENT_ID))
                    .thenReturn(Optional.of(existing));

            UserSegmentProgress result = lessonProgressService.getOrCreateSegmentProgress(USER_ID, testSegment);

            assertThat(result).isSameAs(existing);
            verify(userSegmentProgressRepository, never()).save(any(UserSegmentProgress.class));
            verify(userRepository, never()).getReferenceById(anyLong());
        }

        @Test
        @DisplayName("Create segment progress with default values when none exists")
        void getOrCreateSegmentProgress_Success_Create() {
            when(userSegmentProgressRepository.findByUserIdAndSegmentId(USER_ID, SEGMENT_ID))
                    .thenReturn(Optional.empty());
            when(userRepository.getReferenceById(USER_ID)).thenReturn(testUser);
            when(userSegmentProgressRepository.save(any(UserSegmentProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            UserSegmentProgress result = lessonProgressService.getOrCreateSegmentProgress(USER_ID, testSegment);

            ArgumentCaptor<UserSegmentProgress> captor = ArgumentCaptor.forClass(UserSegmentProgress.class);
            verify(userSegmentProgressRepository).save(captor.capture());
            UserSegmentProgress saved = captor.getValue();
            assertThat(result).isSameAs(saved);
            assertThat(saved.getUser()).isSameAs(testUser);
            assertThat(saved.getLesson()).isSameAs(testLesson);
            assertThat(saved.getSegment()).isSameAs(testSegment);
            assertThat(saved.getDictationAttemptCount()).isZero();
            assertThat(saved.getDictationBestScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getDictationHintUsedCount()).isZero();
            assertThat(saved.getShadowingAttemptCount()).isZero();
            assertThat(saved.getShadowingBestScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getShadowingLatestAudioUrl()).isNull();
        }
    }

    // ========================================================================
    // recordSegmentSubmitted
    // ========================================================================
    @Nested
    @DisplayName("recordSegmentSubmitted")
    class RecordSegmentSubmittedTests {

        private void stubCreateNewLessonProgress() {
            when(userLessonProgressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID)).thenReturn(Optional.empty());
            when(userRepository.getReferenceById(USER_ID)).thenReturn(testUser);
            when(lessonRepository.getReferenceById(LESSON_ID)).thenReturn(testLesson);
            when(userLessonProgressRepository.save(any(UserLessonProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("Record dictation submit creates lesson progress in progress")
        void recordSegmentSubmitted_Success_DictationInProgress() {
            when(lessonSegmentRepository.countByLessonId(LESSON_ID)).thenReturn(4L);
            when(userSegmentProgressRepository.countByUserIdAndLessonIdAndDictationAttemptCountGreaterThan(USER_ID, LESSON_ID, 0))
                    .thenReturn(1L);
            stubCreateNewLessonProgress();

            UserLessonProgress result = lessonProgressService.recordSegmentSubmitted(
                    USER_ID, LESSON_ID, LessonMode.DICTATION, SEGMENT_START_MS);

            assertThat(result.getUser()).isSameAs(testUser);
            assertThat(result.getLesson()).isSameAs(testLesson);
            assertThat(result.getDictationStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
            assertThat(result.getDictationProgressPct()).isEqualByComparingTo("25.00");
            assertThat(result.getDictationLastStartMs()).isEqualTo(SEGMENT_START_MS);
            // Phần shadowing không bị ảnh hưởng
            assertThat(result.getShadowingStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
            assertThat(result.getShadowingProgressPct()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getShadowingLastStartMs()).isNull();
            verify(userSegmentProgressRepository, never())
                    .countByUserIdAndLessonIdAndShadowingAttemptCountGreaterThan(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("Record dictation submit marks lesson completed when all segments are done")
        void recordSegmentSubmitted_Success_DictationCompleted() {
            when(lessonSegmentRepository.countByLessonId(LESSON_ID)).thenReturn(4L);
            when(userSegmentProgressRepository.countByUserIdAndLessonIdAndDictationAttemptCountGreaterThan(USER_ID, LESSON_ID, 0))
                    .thenReturn(4L);
            stubCreateNewLessonProgress();

            UserLessonProgress result = lessonProgressService.recordSegmentSubmitted(
                    USER_ID, LESSON_ID, LessonMode.DICTATION, SEGMENT_START_MS);

            assertThat(result.getDictationStatus()).isEqualTo(ProgressStatus.COMPLETED);
            assertThat(result.getDictationProgressPct()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Record shadowing submit uses shadowing attempt counts and rounds percentage")
        void recordSegmentSubmitted_Success_ShadowingInProgress() {
            when(lessonSegmentRepository.countByLessonId(LESSON_ID)).thenReturn(3L);
            when(userSegmentProgressRepository.countByUserIdAndLessonIdAndShadowingAttemptCountGreaterThan(USER_ID, LESSON_ID, 0))
                    .thenReturn(2L);
            stubCreateNewLessonProgress();

            UserLessonProgress result = lessonProgressService.recordSegmentSubmitted(
                    USER_ID, LESSON_ID, LessonMode.SHADOWING, SEGMENT_START_MS);

            assertThat(result.getShadowingStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
            assertThat(result.getShadowingProgressPct()).isEqualByComparingTo("66.67");
            assertThat(result.getShadowingLastStartMs()).isEqualTo(SEGMENT_START_MS);
            // Phần dictation không bị ảnh hưởng
            assertThat(result.getDictationStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
            assertThat(result.getDictationLastStartMs()).isNull();
            verify(userSegmentProgressRepository, never())
                    .countByUserIdAndLessonIdAndDictationAttemptCountGreaterThan(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("Record shadowing submit marks lesson completed when all segments are done")
        void recordSegmentSubmitted_Success_ShadowingCompleted() {
            when(lessonSegmentRepository.countByLessonId(LESSON_ID)).thenReturn(1L);
            when(userSegmentProgressRepository.countByUserIdAndLessonIdAndShadowingAttemptCountGreaterThan(USER_ID, LESSON_ID, 0))
                    .thenReturn(1L);
            stubCreateNewLessonProgress();

            UserLessonProgress result = lessonProgressService.recordSegmentSubmitted(
                    USER_ID, LESSON_ID, LessonMode.SHADOWING, SEGMENT_START_MS);

            assertThat(result.getShadowingStatus()).isEqualTo(ProgressStatus.COMPLETED);
            assertThat(result.getShadowingProgressPct()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Record submit updates the existing lesson progress without creating a new one")
        void recordSegmentSubmitted_Success_UpdateExisting() {
            UserLessonProgress existing = UserLessonProgress.builder()
                    .id(900L).user(testUser).lesson(testLesson)
                    .shadowingStatus(ProgressStatus.COMPLETED)
                    .shadowingProgressPct(new BigDecimal("100.00"))
                    .shadowingLastStartMs(9000)
                    .dictationStatus(ProgressStatus.IN_PROGRESS)
                    .dictationProgressPct(new BigDecimal("25.00"))
                    .dictationLastStartMs(0)
                    .build();
            when(lessonSegmentRepository.countByLessonId(LESSON_ID)).thenReturn(4L);
            when(userSegmentProgressRepository.countByUserIdAndLessonIdAndDictationAttemptCountGreaterThan(USER_ID, LESSON_ID, 0))
                    .thenReturn(2L);
            when(userLessonProgressRepository.findByUserIdAndLessonId(USER_ID, LESSON_ID)).thenReturn(Optional.of(existing));
            when(userLessonProgressRepository.save(any(UserLessonProgress.class))).thenAnswer(inv -> inv.getArgument(0));

            UserLessonProgress result = lessonProgressService.recordSegmentSubmitted(
                    USER_ID, LESSON_ID, LessonMode.DICTATION, SEGMENT_START_MS);

            assertThat(result).isSameAs(existing);
            assertThat(result.getDictationStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
            assertThat(result.getDictationProgressPct()).isEqualByComparingTo("50.00");
            assertThat(result.getDictationLastStartMs()).isEqualTo(SEGMENT_START_MS);
            // Tiến độ shadowing được giữ nguyên
            assertThat(result.getShadowingStatus()).isEqualTo(ProgressStatus.COMPLETED);
            assertThat(result.getShadowingProgressPct()).isEqualByComparingTo("100.00");
            assertThat(result.getShadowingLastStartMs()).isEqualTo(9000);
            verify(userRepository, never()).getReferenceById(anyLong());
            verify(lessonRepository, never()).getReferenceById(anyLong());
        }
    }

    // ========================================================================
    // replaceWordsAccuracy
    // ========================================================================
    @Nested
    @DisplayName("replaceWordsAccuracy")
    class ReplaceWordsAccuracyTests {

        private UserSegmentProgress progress;

        @BeforeEach
        void initProgress() {
            progress = UserSegmentProgress.builder()
                    .id(500L).user(testUser).lesson(testLesson).segment(testSegment).build();
        }

        @Test
        @DisplayName("Replace words accuracy deletes old rows then saves the new ones")
        void replaceWordsAccuracy_Success() {
            List<SegmentWordAccuracy> words = List.of(
                    SegmentWordAccuracy.builder().userSegmentProgress(progress).word("hello").accuracy(new BigDecimal("95.00")).build(),
                    SegmentWordAccuracy.builder().userSegmentProgress(progress).word("world").accuracy(new BigDecimal("60.00")).build());

            lessonProgressService.replaceWordsAccuracy(progress, words);

            var inOrder = org.mockito.Mockito.inOrder(segmentWordAccuracyRepository);
            inOrder.verify(segmentWordAccuracyRepository).deleteAllByUserSegmentProgressId(500L);
            inOrder.verify(segmentWordAccuracyRepository).saveAll(words);
        }

        @Test
        @DisplayName("Replace words accuracy with an empty list only deletes old rows")
        void replaceWordsAccuracy_Success_Empty() {
            lessonProgressService.replaceWordsAccuracy(progress, List.of());

            verify(segmentWordAccuracyRepository).deleteAllByUserSegmentProgressId(500L);
            verify(segmentWordAccuracyRepository, never()).saveAll(any());
        }
    }
}
