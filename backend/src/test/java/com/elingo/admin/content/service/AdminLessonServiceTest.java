package com.elingo.admin.content.service;

import com.elingo.admin.content.service.impl.AdminLessonServiceImpl;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.lesson.dto.request.AdminLessonSearchRequest;
import com.elingo.learning.lesson.dto.request.LessonRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonResponse;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.learning.lesson.service.LessonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminLessonService")
class AdminLessonServiceTest {

    @Mock
    private LessonService lessonService;

    @InjectMocks
    private AdminLessonServiceImpl adminLessonService;

    private static final Long ADMIN_ID = 1L;
    private static final Long LESSON_ID = 7L;

    private AdminLessonResponse sampleResponse(LessonStatus status) {
        return new AdminLessonResponse(
                LESSON_ID,
                "How to Order Coffee",
                "how-to-order-coffee-12345678",
                "Description",
                false,
                status,
                212_000,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "system/lessons/thumb.png",
                status == LessonStatus.DRAFT ? null : LocalDateTime.now(),
                0,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of(),
                Set.of()
        );
    }

    private LessonRequest sampleRequest() {
        return new LessonRequest(
                "How to Order Coffee",
                "Description",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                212_000,
                "verified/1/thumb.png",
                false,
                Set.of(1L),
                Set.of(2L)
        );
    }

    @Nested
    @DisplayName("searchLessons")
    class SearchLessons {

        @Test
        @DisplayName("should delegate to lessonService when sort field is allowed")
        void shouldDelegate_whenSortIsAllowed() {
            // given
            AdminLessonSearchRequest request = new AdminLessonSearchRequest(
                    "coffee", LessonStatus.DRAFT, false, Set.of(), Set.of());
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            PageResponse<AdminLessonResponse> expected = new PageResponse<>(
                    List.of(sampleResponse(LessonStatus.DRAFT)), 1, 20, 1, 1, true);
            when(lessonService.searchLessonsForAdmin(request, pageable)).thenReturn(expected);

            // when
            PageResponse<AdminLessonResponse> actual = adminLessonService.searchLessons(request, pageable);

            // then
            assertThat(actual).isEqualTo(expected);
            verify(lessonService).searchLessonsForAdmin(request, pageable);
        }

        @Test
        @DisplayName("should throw INVALID_SORT when sort field is not allowed")
        void shouldThrowInvalidSort_whenSortFieldDisallowed() {
            // given
            AdminLessonSearchRequest request = new AdminLessonSearchRequest(
                    null, null, null, null, null);
            Pageable invalidPageable = PageRequest.of(0, 20, Sort.by("unknownField"));

            // when & then
            assertThatThrownBy(() -> adminLessonService.searchLessons(request, invalidPageable))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_SORT));

            verifyNoInteractions(lessonService);
        }
    }

    @Nested
    @DisplayName("getLessonById")
    class GetLessonById {

        @Test
        @DisplayName("should delegate to lessonService.getLessonByIdForAdmin")
        void shouldDelegateToGetLessonByIdForAdmin() {
            // given
            AdminLessonResponse expected = sampleResponse(LessonStatus.PUBLISHED);
            when(lessonService.getLessonByIdForAdmin(LESSON_ID)).thenReturn(expected);

            // when
            AdminLessonResponse actual = adminLessonService.getLessonById(LESSON_ID);

            // then
            assertThat(actual).isEqualTo(expected);
            verify(lessonService).getLessonByIdForAdmin(LESSON_ID);
        }
    }

    @Nested
    @DisplayName("createLesson")
    class CreateLesson {

        @Test
        @DisplayName("should delegate to lessonService.createLesson")
        void shouldDelegateToCreateLesson() {
            // given
            LessonRequest request = sampleRequest();
            AdminLessonResponse expected = sampleResponse(LessonStatus.DRAFT);
            when(lessonService.createLesson(request)).thenReturn(expected);

            // when
            AdminLessonResponse actual = adminLessonService.createLesson(ADMIN_ID, request);

            // then
            assertThat(actual).isEqualTo(expected);
            verify(lessonService).createLesson(request);
        }
    }

    @Nested
    @DisplayName("updateLesson")
    class UpdateLesson {

        @Test
        @DisplayName("should delegate to lessonService.updateLesson")
        void shouldDelegateToUpdateLesson() {
            // given
            LessonRequest request = sampleRequest();
            AdminLessonResponse expected = sampleResponse(LessonStatus.DRAFT);
            when(lessonService.updateLesson(LESSON_ID, request)).thenReturn(expected);

            // when
            AdminLessonResponse actual = adminLessonService.updateLesson(ADMIN_ID, LESSON_ID, request);

            // then
            assertThat(actual).isEqualTo(expected);
            verify(lessonService).updateLesson(LESSON_ID, request);
        }
    }

    @Nested
    @DisplayName("publishLesson")
    class PublishLesson {

        @Test
        @DisplayName("should delegate to lessonService.publishLesson")
        void shouldDelegateToPublishLesson() {
            // given
            AdminLessonResponse expected = sampleResponse(LessonStatus.PUBLISHED);
            when(lessonService.publishLesson(LESSON_ID)).thenReturn(expected);

            // when
            AdminLessonResponse actual = adminLessonService.publishLesson(ADMIN_ID, LESSON_ID);

            // then
            assertThat(actual).isEqualTo(expected);
            verify(lessonService).publishLesson(LESSON_ID);
        }
    }

    @Nested
    @DisplayName("unpublishLesson")
    class UnpublishLesson {

        @Test
        @DisplayName("should delegate to lessonService.unpublishLesson")
        void shouldDelegateToUnpublishLesson() {
            // given
            AdminLessonResponse expected = sampleResponse(LessonStatus.DRAFT);
            when(lessonService.unpublishLesson(LESSON_ID)).thenReturn(expected);

            // when
            AdminLessonResponse actual = adminLessonService.unpublishLesson(ADMIN_ID, LESSON_ID);

            // then
            assertThat(actual).isEqualTo(expected);
            verify(lessonService).unpublishLesson(LESSON_ID);
        }
    }

    @Nested
    @DisplayName("archiveLesson")
    class ArchiveLesson {

        @Test
        @DisplayName("should delegate to lessonService.archiveLesson")
        void shouldDelegateToArchiveLesson() {
            // given
            AdminLessonResponse expected = sampleResponse(LessonStatus.ARCHIVED);
            when(lessonService.archiveLesson(LESSON_ID)).thenReturn(expected);

            // when
            AdminLessonResponse actual = adminLessonService.archiveLesson(ADMIN_ID, LESSON_ID);

            // then
            assertThat(actual).isEqualTo(expected);
            verify(lessonService).archiveLesson(LESSON_ID);
        }
    }
}
