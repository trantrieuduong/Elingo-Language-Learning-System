package com.elingo.admin.content.service;

import com.elingo.admin.content.service.impl.AdminLessonSegmentServiceImpl;
import com.elingo.learning.lesson.dto.request.CreateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.request.DeleteLessonSegmentsRequest;
import com.elingo.learning.lesson.dto.request.UpdateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonSegmentResponse;
import com.elingo.learning.lesson.service.LessonSegmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminLessonSegmentService")
class AdminLessonSegmentServiceTest {

    @Mock
    private LessonSegmentService lessonSegmentService;

    @InjectMocks
    private AdminLessonSegmentServiceImpl adminLessonSegmentService;

    private static final Long ADMIN_ID = 1L;
    private static final Long LESSON_ID = 7L;
    private static final Long SEGMENT_ID = 11L;

    private AdminLessonSegmentResponse sampleResponse() {
        return new AdminLessonSegmentResponse(
                SEGMENT_ID, LESSON_ID, 1000, 5000,
                "Hello world", "hello world", "Xin chào thế giới",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("getSegmentsByLessonId")
    class GetSegmentsByLessonId {

        @Test
        @DisplayName("should delegate to lessonSegmentService.getSegmentsByLessonIdForAdmin")
        void shouldDelegateToGetSegmentsByLessonIdForAdmin() {
            // given
            List<AdminLessonSegmentResponse> expected = List.of(sampleResponse());
            when(lessonSegmentService.getSegmentsByLessonIdForAdmin(LESSON_ID)).thenReturn(expected);

            // when
            List<AdminLessonSegmentResponse> actual = adminLessonSegmentService.getSegmentsByLessonId(LESSON_ID);

            // then
            assertThat(actual).isEqualTo(expected);
            verify(lessonSegmentService).getSegmentsByLessonIdForAdmin(LESSON_ID);
        }
    }

    @Nested
    @DisplayName("createSegment")
    class CreateSegment {

        @Test
        @DisplayName("should delegate to lessonSegmentService.createSegment")
        void shouldDelegateToCreateSegment() {
            // given
            CreateLessonSegmentRequest request = new CreateLessonSegmentRequest(
                    LESSON_ID, 1000, 5000, "Hello world", "Xin chào thế giới");
            AdminLessonSegmentResponse expected = sampleResponse();
            when(lessonSegmentService.createSegment(request)).thenReturn(expected);

            // when
            AdminLessonSegmentResponse actual = adminLessonSegmentService.createSegment(ADMIN_ID, request);

            // then
            assertThat(actual).isEqualTo(expected);
            verify(lessonSegmentService).createSegment(request);
        }
    }

    @Nested
    @DisplayName("updateSegment")
    class UpdateSegment {

        @Test
        @DisplayName("should delegate to lessonSegmentService.updateSegment")
        void shouldDelegateToUpdateSegment() {
            // given
            UpdateLessonSegmentRequest request = new UpdateLessonSegmentRequest(
                    1000, 5000, "Hello world", "Xin chào thế giới");
            AdminLessonSegmentResponse expected = sampleResponse();
            when(lessonSegmentService.updateSegment(SEGMENT_ID, request)).thenReturn(expected);

            // when
            AdminLessonSegmentResponse actual = adminLessonSegmentService.updateSegment(ADMIN_ID, SEGMENT_ID, request);

            // then
            assertThat(actual).isEqualTo(expected);
            verify(lessonSegmentService).updateSegment(SEGMENT_ID, request);
        }
    }

    @Nested
    @DisplayName("deleteSegments")
    class DeleteSegments {

        @Test
        @DisplayName("should delegate to lessonSegmentService.deleteSegments")
        void shouldDelegateToDeleteSegments() {
            // given
            DeleteLessonSegmentsRequest request = new DeleteLessonSegmentsRequest(Set.of(11L, 12L));

            // when
            adminLessonSegmentService.deleteSegments(ADMIN_ID, LESSON_ID, request);

            // then
            verify(lessonSegmentService).deleteSegments(LESSON_ID, request);
        }
    }
}
