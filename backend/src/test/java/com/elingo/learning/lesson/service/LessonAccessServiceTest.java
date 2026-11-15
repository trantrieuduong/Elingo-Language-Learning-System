package com.elingo.learning.lesson.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.learning.lesson.repository.LessonRepository;
import com.elingo.learning.lesson.service.impl.LessonAccessServiceImpl;
import com.elingo.premium.service.PremiumAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LessonAccessServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private PremiumAccessService premiumAccessService;

    @InjectMocks
    private LessonAccessServiceImpl lessonAccessService;

    private static final Long USER_ID = 1L;
    private static final Long LESSON_ID = 10L;
    private static final String SLUG = "ted-talk-on-habits";

    private Lesson freeLesson;
    private Lesson premiumLesson;

    @BeforeEach
    void setUp() {
        freeLesson = Lesson.builder()
                .id(LESSON_ID)
                .title("Free lesson")
                .slug(SLUG)
                .status(LessonStatus.PUBLISHED)
                .isPremium(false)
                .sourceUrl("https://www.youtube.com/watch?v=abc")
                .build();

        premiumLesson = Lesson.builder()
                .id(LESSON_ID)
                .title("Premium lesson")
                .slug(SLUG)
                .status(LessonStatus.PUBLISHED)
                .isPremium(true)
                .sourceUrl("https://www.youtube.com/watch?v=abc")
                .build();
    }

    // ========================================================================
    // validateLessonAccess (by id)
    // ========================================================================
    @Nested
    @DisplayName("validateLessonAccess by id")
    class ValidateByIdTests {

        @Test
        @DisplayName("Validate access successfully for a free lesson")
        void validateLessonAccess_Success_FreeLesson() {
            when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                    .thenReturn(Optional.of(freeLesson));

            Lesson result = lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID);

            assertThat(result).isSameAs(freeLesson);
            verifyNoInteractions(premiumAccessService);
        }

        @Test
        @DisplayName("Validate access successfully for a free lesson when user is anonymous")
        void validateLessonAccess_Success_FreeLessonAnonymous() {
            when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                    .thenReturn(Optional.of(freeLesson));

            Lesson result = lessonAccessService.validateLessonAccess(null, LESSON_ID);

            assertThat(result).isSameAs(freeLesson);
            verifyNoInteractions(premiumAccessService);
        }

        @Test
        @DisplayName("Validate access treats null premium flag as a free lesson")
        void validateLessonAccess_Success_NullPremiumFlag() {
            freeLesson.setIsPremium(null);
            when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                    .thenReturn(Optional.of(freeLesson));

            Lesson result = lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID);

            assertThat(result).isSameAs(freeLesson);
            verifyNoInteractions(premiumAccessService);
        }

        @Test
        @DisplayName("Validate access successfully for a premium lesson when user has premium")
        void validateLessonAccess_Success_PremiumLesson() {
            when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                    .thenReturn(Optional.of(premiumLesson));

            Lesson result = lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID);

            assertThat(result).isSameAs(premiumLesson);
            verify(premiumAccessService).requirePremium(USER_ID);
        }

        @Test
        @DisplayName("Validate access failed: Premium lesson but user has no premium")
        void validateLessonAccess_Fail_PremiumRequired() {
            when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                    .thenReturn(Optional.of(premiumLesson));
            doThrow(new AppException(AppError.PREMIUM_REQUIRED)).when(premiumAccessService).requirePremium(USER_ID);

            assertThatThrownBy(() -> lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PREMIUM_REQUIRED));
        }

        @Test
        @DisplayName("Validate access checks premium for anonymous user on a premium lesson")
        void validateLessonAccess_Fail_PremiumLessonAnonymous() {
            when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                    .thenReturn(Optional.of(premiumLesson));
            doThrow(new AppException(AppError.PREMIUM_REQUIRED)).when(premiumAccessService).requirePremium(null);

            assertThatThrownBy(() -> lessonAccessService.validateLessonAccess(null, LESSON_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PREMIUM_REQUIRED));
        }

        @Test
        @DisplayName("Validate access failed: Lesson not found or not published")
        void validateLessonAccess_Fail_LessonNotFound() {
            when(lessonRepository.findByIdAndStatus(LESSON_ID, LessonStatus.PUBLISHED))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonAccessService.validateLessonAccess(USER_ID, LESSON_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_NOT_FOUND));

            verify(premiumAccessService, never()).requirePremium(USER_ID);
        }
    }

    // ========================================================================
    // validateLessonAccess (by slug)
    // ========================================================================
    @Nested
    @DisplayName("validateLessonAccess by slug")
    class ValidateBySlugTests {

        @Test
        @DisplayName("Validate access by slug successfully for a free lesson")
        void validateLessonAccessBySlug_Success_FreeLesson() {
            when(lessonRepository.findBySlugAndStatus(SLUG, LessonStatus.PUBLISHED))
                    .thenReturn(Optional.of(freeLesson));

            Lesson result = lessonAccessService.validateLessonAccess(USER_ID, SLUG);

            assertThat(result).isSameAs(freeLesson);
            verifyNoInteractions(premiumAccessService);
        }

        @Test
        @DisplayName("Validate access by slug successfully for a premium lesson when user has premium")
        void validateLessonAccessBySlug_Success_PremiumLesson() {
            when(lessonRepository.findBySlugAndStatus(SLUG, LessonStatus.PUBLISHED))
                    .thenReturn(Optional.of(premiumLesson));

            Lesson result = lessonAccessService.validateLessonAccess(USER_ID, SLUG);

            assertThat(result).isSameAs(premiumLesson);
            verify(premiumAccessService).requirePremium(USER_ID);
        }

        @Test
        @DisplayName("Validate access by slug failed: Premium lesson but user has no premium")
        void validateLessonAccessBySlug_Fail_PremiumRequired() {
            when(lessonRepository.findBySlugAndStatus(SLUG, LessonStatus.PUBLISHED))
                    .thenReturn(Optional.of(premiumLesson));
            doThrow(new AppException(AppError.PREMIUM_REQUIRED)).when(premiumAccessService).requirePremium(USER_ID);

            assertThatThrownBy(() -> lessonAccessService.validateLessonAccess(USER_ID, SLUG))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PREMIUM_REQUIRED));
        }

        @Test
        @DisplayName("Validate access by slug failed: Lesson not found or not published")
        void validateLessonAccessBySlug_Fail_LessonNotFound() {
            when(lessonRepository.findBySlugAndStatus(SLUG, LessonStatus.PUBLISHED))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonAccessService.validateLessonAccess(USER_ID, SLUG))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LESSON_NOT_FOUND));

            verifyNoInteractions(premiumAccessService);
        }
    }
}
