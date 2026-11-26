package com.elingo.admin.content.controller;

import com.elingo.admin.content.service.AdminLessonSegmentService;
import com.elingo.admin.content.service.AdminLessonService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.learning.lesson.dto.request.AdminLessonSearchRequest;
import com.elingo.learning.lesson.dto.request.DeleteLessonSegmentsRequest;
import com.elingo.learning.lesson.dto.request.LessonRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonResponse;
import com.elingo.learning.lesson.dto.response.AdminLessonSegmentResponse;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminLessonController")
class AdminLessonControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminLessonService adminLessonService;

    @Mock
    private AdminLessonSegmentService adminLessonSegmentService;

    @InjectMocks
    private AdminLessonController adminLessonController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long ADMIN_ID = 1L;
    private static final Long LESSON_ID = 7L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminLessonController)
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
                                return ADMIN_ID;
                            }
                        }
                )
                .build();
    }

    private AdminLessonResponse sampleLessonResponse(LessonStatus status) {
        return new AdminLessonResponse(
                LESSON_ID,
                "How to Order Coffee",
                "how-to-order-coffee-12345678",
                "A short conversation",
                false,
                status,
                212_000,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                null,
                status == LessonStatus.DRAFT ? null : LocalDateTime.now(),
                0,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of(),
                Set.of()
        );
    }

    private LessonRequest validLessonRequest() {
        return new LessonRequest(
                "How to Order Coffee",
                "A short conversation",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                212_000,
                null,
                false,
                Set.of(1L),
                Set.of(2L)
        );
    }

    @Nested
    @DisplayName("GET /admin/lessons")
    class SearchLessons {

        @Test
        @DisplayName("should return 200 with paginated lessons")
        void shouldReturn200_whenValid() throws Exception {
            PageResponse<AdminLessonResponse> page = new PageResponse<>(
                    List.of(sampleLessonResponse(LessonStatus.DRAFT)), 1, 20, 1, 1, true);
            when(adminLessonService.searchLessons(any(AdminLessonSearchRequest.class), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/admin/lessons").param("keyword", "Coffee"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].id").value(LESSON_ID));

            verify(adminLessonService).searchLessons(any(AdminLessonSearchRequest.class), any(Pageable.class));
        }

        @Test
        @DisplayName("should return 400 INVALID_SORT when sort field is invalid")
        void shouldReturn400_whenSortInvalid() throws Exception {
            when(adminLessonService.searchLessons(any(AdminLessonSearchRequest.class), any(Pageable.class)))
                    .thenThrow(new AppException(AppError.INVALID_SORT));

            mockMvc.perform(get("/admin/lessons").param("sort", "badField"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_SORT"));
        }
    }

    @Nested
    @DisplayName("GET /admin/lessons/{lessonId}")
    class GetLessonById {

        @Test
        @DisplayName("should return 200 with lesson details")
        void shouldReturn200_whenFound() throws Exception {
            when(adminLessonService.getLessonById(LESSON_ID))
                    .thenReturn(sampleLessonResponse(LessonStatus.PUBLISHED));

            mockMvc.perform(get("/admin/lessons/{lessonId}", LESSON_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(LESSON_ID))
                    .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        }

        @Test
        @DisplayName("should return 404 LESSON_NOT_FOUND when lesson missing")
        void shouldReturn404_whenNotFound() throws Exception {
            when(adminLessonService.getLessonById(LESSON_ID))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            mockMvc.perform(get("/admin/lessons/{lessonId}", LESSON_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /admin/lessons")
    class CreateLesson {

        @Test
        @DisplayName("should return 200 when payload is valid")
        void shouldReturn200_whenValid() throws Exception {
            LessonRequest request = validLessonRequest();
            when(adminLessonService.createLesson(eq(ADMIN_ID), any(LessonRequest.class)))
                    .thenReturn(sampleLessonResponse(LessonStatus.DRAFT));

            mockMvc.perform(post("/admin/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"));

            verify(adminLessonService).createLesson(eq(ADMIN_ID), any(LessonRequest.class));
        }

        @Test
        @DisplayName("should return 400 when required fields fail validation")
        void shouldReturn400_whenValidationFails() throws Exception {
            LessonRequest invalid = new LessonRequest(
                    "   ", null, "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                    null, null, false, Set.of(), Set.of());

            mockMvc.perform(post("/admin/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(adminLessonService);
        }

        @Test
        @DisplayName("should return 400 SOURCE_URL_INVALID when service rejects sourceUrl")
        void shouldReturn400_whenSourceUrlInvalid() throws Exception {
            LessonRequest request = validLessonRequest();
            when(adminLessonService.createLesson(eq(ADMIN_ID), any(LessonRequest.class)))
                    .thenThrow(new AppException(AppError.SOURCE_URL_INVALID));

            mockMvc.perform(post("/admin/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("SOURCE_URL_INVALID"));
        }
    }

    @Nested
    @DisplayName("PUT /admin/lessons/{lessonId}")
    class UpdateLesson {

        @Test
        @DisplayName("should return 200 when update succeeds")
        void shouldReturn200_whenValid() throws Exception {
            LessonRequest request = validLessonRequest();
            when(adminLessonService.updateLesson(eq(ADMIN_ID), eq(LESSON_ID), any(LessonRequest.class)))
                    .thenReturn(sampleLessonResponse(LessonStatus.DRAFT));

            mockMvc.perform(put("/admin/lessons/{lessonId}", LESSON_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(LESSON_ID));
        }

        @Test
        @DisplayName("should return 400 SEGMENT_EXCEEDS_DURATION when durationMs is smaller than existing segment")
        void shouldReturn400_whenDurationTooSmall() throws Exception {
            LessonRequest request = validLessonRequest();
            when(adminLessonService.updateLesson(eq(ADMIN_ID), eq(LESSON_ID), any(LessonRequest.class)))
                    .thenThrow(new AppException(AppError.SEGMENT_EXCEEDS_DURATION));

            mockMvc.perform(put("/admin/lessons/{lessonId}", LESSON_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("SEGMENT_EXCEEDS_DURATION"));
        }
    }

    @Nested
    @DisplayName("Status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("PUT /admin/lessons/{lessonId}/publish should return 200")
        void publish_shouldReturn200() throws Exception {
            when(adminLessonService.publishLesson(ADMIN_ID, LESSON_ID))
                    .thenReturn(sampleLessonResponse(LessonStatus.PUBLISHED));

            mockMvc.perform(put("/admin/lessons/{lessonId}/publish", LESSON_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        }

        @Test
        @DisplayName("PUT /admin/lessons/{lessonId}/publish should return 400 LESSON_PUBLISHED_FAILED when no segments")
        void publish_shouldReturn400_whenNoSegments() throws Exception {
            when(adminLessonService.publishLesson(ADMIN_ID, LESSON_ID))
                    .thenThrow(new AppException(AppError.LESSON_PUBLISHED_FAILED));

            mockMvc.perform(put("/admin/lessons/{lessonId}/publish", LESSON_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_PUBLISHED_FAILED"));
        }

        @Test
        @DisplayName("PUT /admin/lessons/{lessonId}/unpublish should return 200")
        void unpublish_shouldReturn200() throws Exception {
            when(adminLessonService.unpublishLesson(ADMIN_ID, LESSON_ID))
                    .thenReturn(sampleLessonResponse(LessonStatus.DRAFT));

            mockMvc.perform(put("/admin/lessons/{lessonId}/unpublish", LESSON_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("DRAFT"));
        }

        @Test
        @DisplayName("PUT /admin/lessons/{lessonId}/archive should return 200")
        void archive_shouldReturn200() throws Exception {
            when(adminLessonService.archiveLesson(ADMIN_ID, LESSON_ID))
                    .thenReturn(sampleLessonResponse(LessonStatus.ARCHIVED));

            mockMvc.perform(put("/admin/lessons/{lessonId}/archive", LESSON_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
        }
    }

    @Nested
    @DisplayName("GET /admin/lessons/{lessonId}/segments")
    class GetSegments {

        @Test
        @DisplayName("should return 200 with segments list")
        void shouldReturn200_whenFound() throws Exception {
            AdminLessonSegmentResponse seg = new AdminLessonSegmentResponse(
                    11L, LESSON_ID, 0, 5000, "Hello", "hello", "Xin chào",
                    LocalDateTime.now(), LocalDateTime.now());
            when(adminLessonSegmentService.getSegmentsByLessonId(LESSON_ID)).thenReturn(List.of(seg));

            mockMvc.perform(get("/admin/lessons/{lessonId}/segments", LESSON_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].id").value(11L));
        }

        @Test
        @DisplayName("should return 404 LESSON_NOT_FOUND when lesson missing")
        void shouldReturn404_whenLessonMissing() throws Exception {
            when(adminLessonSegmentService.getSegmentsByLessonId(LESSON_ID))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            mockMvc.perform(get("/admin/lessons/{lessonId}/segments", LESSON_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("DELETE /admin/lessons/{lessonId}/segments")
    class DeleteSegments {

        @Test
        @DisplayName("should return 200 when bulk delete succeeds")
        void shouldReturn200_whenValid() throws Exception {
            DeleteLessonSegmentsRequest request = new DeleteLessonSegmentsRequest(Set.of(11L, 12L));
            doNothing().when(adminLessonSegmentService).deleteSegments(eq(ADMIN_ID), eq(LESSON_ID), any());

            mockMvc.perform(delete("/admin/lessons/{lessonId}/segments", LESSON_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(adminLessonSegmentService).deleteSegments(eq(ADMIN_ID), eq(LESSON_ID), any());
        }

        @Test
        @DisplayName("should return 400 when segmentIds is empty")
        void shouldReturn400_whenSegmentIdsEmpty() throws Exception {
            DeleteLessonSegmentsRequest invalid = new DeleteLessonSegmentsRequest(Set.of());

            mockMvc.perform(delete("/admin/lessons/{lessonId}/segments", LESSON_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(adminLessonSegmentService);
        }

        @Test
        @DisplayName("should return 400 LESSON_SEGMENT_REQUIRED when deleting last segment of published lesson")
        void shouldReturn400_whenPublishedNeedsSegment() throws Exception {
            DeleteLessonSegmentsRequest request = new DeleteLessonSegmentsRequest(Set.of(11L));
            doThrow(new AppException(AppError.LESSON_SEGMENT_REQUIRED))
                    .when(adminLessonSegmentService).deleteSegments(eq(ADMIN_ID), eq(LESSON_ID), any());

            mockMvc.perform(delete("/admin/lessons/{lessonId}/segments", LESSON_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_SEGMENT_REQUIRED"));
        }

        @Test
        @DisplayName("should return 404 LESSON_SEGMENT_NOT_FOUND when segment does not belong to lesson")
        void shouldReturn404_whenSegmentNotFound() throws Exception {
            DeleteLessonSegmentsRequest request = new DeleteLessonSegmentsRequest(Set.of(99L));
            doThrow(new AppException(AppError.LESSON_SEGMENT_NOT_FOUND))
                    .when(adminLessonSegmentService).deleteSegments(eq(ADMIN_ID), eq(LESSON_ID), any());

            mockMvc.perform(delete("/admin/lessons/{lessonId}/segments", LESSON_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_SEGMENT_NOT_FOUND"));
        }
    }
}
