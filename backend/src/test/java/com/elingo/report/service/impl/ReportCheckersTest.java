package com.elingo.report.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.community.repository.CommentRepository;
import com.elingo.community.repository.PostRepository;
import com.elingo.learning.lesson.service.LessonAccessService;
import com.elingo.report.entity.ReportTargetType;
import com.elingo.user.repository.UserRepository;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.service.DeckAccessPolicy;
import com.elingo.vocabulary.support.TestDeckAccessView;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ReportCheckersTest {

    private static final Long REPORTER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long TARGET_ID = 100L;

    @Nested
    @DisplayName("UserReportChecker")
    class UserReportCheckerTests {

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private UserReportChecker userReportChecker;

        @Test
        @DisplayName("supports returns ReportTargetType.USER")
        void supports_ReturnsUser() {
            assertThat(userReportChecker.supports()).isEqualTo(ReportTargetType.USER);
        }

        @Test
        @DisplayName("check succeeds when target user exists and is not reporter")
        void check_Success() {
            when(userRepository.existsById(OTHER_USER_ID)).thenReturn(true);

            assertThatCode(() -> userReportChecker.check(OTHER_USER_ID, REPORTER_ID))
                    .doesNotThrowAnyException();

            verify(userRepository).existsById(OTHER_USER_ID);
        }

        @Test
        @DisplayName("check throws CANNOT_REPORT_SELF when reporter reports their own account")
        void check_ThrowCannotReportSelf() {
            assertThatThrownBy(() -> userReportChecker.check(REPORTER_ID, REPORTER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_REPORT_SELF));
        }

        @Test
        @DisplayName("check throws USER_NOT_FOUND when target user does not exist")
        void check_ThrowUserNotFound() {
            when(userRepository.existsById(OTHER_USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> userReportChecker.check(OTHER_USER_ID, REPORTER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("CommentReportChecker")
    class CommentReportCheckerTests {

        @Mock
        private CommentRepository commentRepository;

        @InjectMocks
        private CommentReportChecker commentReportChecker;

        @Test
        @DisplayName("supports returns ReportTargetType.COMMENT")
        void supports_ReturnsComment() {
            assertThat(commentReportChecker.supports()).isEqualTo(ReportTargetType.COMMENT);
        }

        @Test
        @DisplayName("check succeeds when comment exists and belongs to another user")
        void check_Success() {
            when(commentRepository.findVisibleCommentOwnerId(TARGET_ID)).thenReturn(Optional.of(OTHER_USER_ID));

            assertThatCode(() -> commentReportChecker.check(TARGET_ID, REPORTER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("check throws COMMENT_NOT_FOUND when comment does not exist or is invisible")
        void check_ThrowCommentNotFound() {
            when(commentRepository.findVisibleCommentOwnerId(TARGET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentReportChecker.check(TARGET_ID, REPORTER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.COMMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("check throws CANNOT_REPORT_SELF when reporter reports their own comment")
        void check_ThrowCannotReportSelf() {
            when(commentRepository.findVisibleCommentOwnerId(TARGET_ID)).thenReturn(Optional.of(REPORTER_ID));

            assertThatThrownBy(() -> commentReportChecker.check(TARGET_ID, REPORTER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_REPORT_SELF));
        }
    }

    @Nested
    @DisplayName("PostReportChecker")
    class PostReportCheckerTests {

        @Mock
        private PostRepository postRepository;

        @InjectMocks
        private PostReportChecker postReportChecker;

        @Test
        @DisplayName("supports returns ReportTargetType.POST")
        void supports_ReturnsPost() {
            assertThat(postReportChecker.supports()).isEqualTo(ReportTargetType.POST);
        }

        @Test
        @DisplayName("check succeeds when post exists and belongs to another user")
        void check_Success() {
            when(postRepository.findPublishedPostOwnerId(TARGET_ID)).thenReturn(Optional.of(OTHER_USER_ID));

            assertThatCode(() -> postReportChecker.check(TARGET_ID, REPORTER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("check throws POST_NOT_FOUND when post does not exist or is not published")
        void check_ThrowPostNotFound() {
            when(postRepository.findPublishedPostOwnerId(TARGET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postReportChecker.check(TARGET_ID, REPORTER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("check throws CANNOT_REPORT_SELF when reporter reports their own post")
        void check_ThrowCannotReportSelf() {
            when(postRepository.findPublishedPostOwnerId(TARGET_ID)).thenReturn(Optional.of(REPORTER_ID));

            assertThatThrownBy(() -> postReportChecker.check(TARGET_ID, REPORTER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_REPORT_SELF));
        }
    }

    @Nested
    @DisplayName("DeckReportChecker")
    class DeckReportCheckerTests {

        @Mock
        private DeckRepository deckRepository;

        @Mock
        private DeckAccessPolicy deckAccessPolicy;

        @InjectMocks
        private DeckReportChecker deckReportChecker;

        @Test
        @DisplayName("supports returns ReportTargetType.DECK")
        void supports_ReturnsDeck() {
            assertThat(deckReportChecker.supports()).isEqualTo(ReportTargetType.DECK);
        }

        @Test
        @DisplayName("check succeeds for published system deck")
        void check_Success_SystemDeck() {
            TestDeckAccessView view = TestDeckAccessView.published(TARGET_ID);
            when(deckRepository.findAccessViewById(TARGET_ID)).thenReturn(Optional.of(view));
            doNothing().when(deckAccessPolicy).assertCanView(view, REPORTER_ID);

            assertThatCode(() -> deckReportChecker.check(TARGET_ID, REPORTER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("check succeeds for user deck owned by another user")
        void check_Success_OtherUserDeck() {
            TestDeckAccessView view = TestDeckAccessView.ownedBy(TARGET_ID, DeckStatus.PUBLISHED, OTHER_USER_ID);
            when(deckRepository.findAccessViewById(TARGET_ID)).thenReturn(Optional.of(view));
            doNothing().when(deckAccessPolicy).assertCanView(view, REPORTER_ID);

            assertThatCode(() -> deckReportChecker.check(TARGET_ID, REPORTER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("check throws DECK_NOT_FOUND when deck does not exist")
        void check_ThrowDeckNotFound() {
            when(deckRepository.findAccessViewById(TARGET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deckReportChecker.check(TARGET_ID, REPORTER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_NOT_FOUND));
        }

        @Test
        @DisplayName("check throws CANNOT_REPORT_SELF when deck is owned by reporter")
        void check_ThrowCannotReportSelf() {
            TestDeckAccessView view = TestDeckAccessView.ownedBy(TARGET_ID, DeckStatus.DRAFT, REPORTER_ID);
            when(deckRepository.findAccessViewById(TARGET_ID)).thenReturn(Optional.of(view));
            doNothing().when(deckAccessPolicy).assertCanView(view, REPORTER_ID);

            assertThatThrownBy(() -> deckReportChecker.check(TARGET_ID, REPORTER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_REPORT_SELF));
        }

        @Test
        @DisplayName("check propagates exception when DeckAccessPolicy denies viewing")
        void check_ThrowExceptionWhenPolicyDenies() {
            TestDeckAccessView view = TestDeckAccessView.ownedBy(TARGET_ID, DeckStatus.DRAFT, OTHER_USER_ID);
            when(deckRepository.findAccessViewById(TARGET_ID)).thenReturn(Optional.of(view));
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(deckAccessPolicy).assertCanView(view, REPORTER_ID);

            assertThatThrownBy(() -> deckReportChecker.check(TARGET_ID, REPORTER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DECK_ACCESS_DENIED));
        }
    }

    @Nested
    @DisplayName("LessonReportChecker")
    class LessonReportCheckerTests {

        @Mock
        private LessonAccessService lessonAccessService;

        @InjectMocks
        private LessonReportChecker lessonReportChecker;

        @Test
        @DisplayName("supports returns ReportTargetType.LESSON")
        void supports_ReturnsLesson() {
            assertThat(lessonReportChecker.supports()).isEqualTo(ReportTargetType.LESSON);
        }

        @Test
        @DisplayName("check succeeds when lesson access is valid")
        void check_Success() {
            when(lessonAccessService.validateLessonAccess(REPORTER_ID, TARGET_ID)).thenReturn(null);

            assertThatCode(() -> lessonReportChecker.check(TARGET_ID, REPORTER_ID))
                    .doesNotThrowAnyException();

            verify(lessonAccessService).validateLessonAccess(REPORTER_ID, TARGET_ID);
        }

        @Test
        @DisplayName("check propagates exception when lesson access service throws")
        void check_ThrowExceptionWhenLessonAccessInvalid() {
            doThrow(new AppException(AppError.LESSON_NOT_FOUND))
                    .when(lessonAccessService).validateLessonAccess(REPORTER_ID, TARGET_ID);

            assertThatThrownBy(() -> lessonReportChecker.check(TARGET_ID, REPORTER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.LESSON_NOT_FOUND));
        }
    }
}
