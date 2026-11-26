package com.elingo.admin.content.controller;

import com.elingo.admin.content.service.AdminLessonSegmentService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.learning.lesson.dto.request.CreateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.request.UpdateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonSegmentResponse;
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
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminLessonSegmentController")
class AdminLessonSegmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminLessonSegmentService adminLessonSegmentService;

    @InjectMocks
    private AdminLessonSegmentController adminLessonSegmentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long ADMIN_ID = 1L;
    private static final Long LESSON_ID = 7L;
    private static final Long SEGMENT_ID = 11L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminLessonSegmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
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
                })
                .build();
    }

    private AdminLessonSegmentResponse sampleResponse() {
        return new AdminLessonSegmentResponse(
                SEGMENT_ID, LESSON_ID, 1500, 6200,
                "Hi, can I get a large coffee, please?",
                "hi can i get a large coffee please",
                "Xin chào, cho tôi một ly cà phê lớn nhé?",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("POST /admin/lesson-segments")
    class CreateSegment {

        @Test
        @DisplayName("should return 200 when payload is valid")
        void shouldReturn200_whenValid() throws Exception {
            CreateLessonSegmentRequest request = new CreateLessonSegmentRequest(
                    LESSON_ID, 1500, 6200,
                    "Hi, can I get a large coffee, please?",
                    "Xin chào, cho tôi một ly cà phê lớn nhé?");
            when(adminLessonSegmentService.createSegment(eq(ADMIN_ID), any(CreateLessonSegmentRequest.class)))
                    .thenReturn(sampleResponse());

            mockMvc.perform(post("/admin/lesson-segments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(SEGMENT_ID))
                    .andExpect(jsonPath("$.data.transcriptNormalized").value("hi can i get a large coffee please"));

            verify(adminLessonSegmentService).createSegment(eq(ADMIN_ID), any(CreateLessonSegmentRequest.class));
        }

        @Test
        @DisplayName("should return 400 when validation fails")
        void shouldReturn400_whenValidationFails() throws Exception {
            CreateLessonSegmentRequest invalid = new CreateLessonSegmentRequest(
                    LESSON_ID, -1, 0, "  ", "");

            mockMvc.perform(post("/admin/lesson-segments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(adminLessonSegmentService);
        }

        @Test
        @DisplayName("should return 400 SEGMENT_TIME_OVERLAP when service throws overlap")
        void shouldReturn400_whenOverlap() throws Exception {
            CreateLessonSegmentRequest request = new CreateLessonSegmentRequest(
                    LESSON_ID, 1500, 6200, "Hello", "Xin chào");
            when(adminLessonSegmentService.createSegment(eq(ADMIN_ID), any(CreateLessonSegmentRequest.class)))
                    .thenThrow(new AppException(AppError.SEGMENT_TIME_OVERLAP));

            mockMvc.perform(post("/admin/lesson-segments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("SEGMENT_TIME_OVERLAP"));
        }

        @Test
        @DisplayName("should return 404 LESSON_NOT_FOUND when lesson does not exist")
        void shouldReturn404_whenLessonNotFound() throws Exception {
            CreateLessonSegmentRequest request = new CreateLessonSegmentRequest(
                    LESSON_ID, 1500, 6200, "Hello", "Xin chào");
            when(adminLessonSegmentService.createSegment(eq(ADMIN_ID), any(CreateLessonSegmentRequest.class)))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            mockMvc.perform(post("/admin/lesson-segments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("PUT /admin/lesson-segments/{segmentId}")
    class UpdateSegment {

        @Test
        @DisplayName("should return 200 when update succeeds")
        void shouldReturn200_whenValid() throws Exception {
            UpdateLessonSegmentRequest request = new UpdateLessonSegmentRequest(
                    1500, 6200, "Updated text", "Văn bản mới");
            when(adminLessonSegmentService.updateSegment(eq(ADMIN_ID), eq(SEGMENT_ID), any(UpdateLessonSegmentRequest.class)))
                    .thenReturn(sampleResponse());

            mockMvc.perform(put("/admin/lesson-segments/{segmentId}", SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(SEGMENT_ID));

            verify(adminLessonSegmentService).updateSegment(eq(ADMIN_ID), eq(SEGMENT_ID), any(UpdateLessonSegmentRequest.class));
        }

        @Test
        @DisplayName("should return 400 when validation fails")
        void shouldReturn400_whenValidationFails() throws Exception {
            UpdateLessonSegmentRequest invalid = new UpdateLessonSegmentRequest(
                    0, 0, "", "");

            mockMvc.perform(put("/admin/lesson-segments/{segmentId}", SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(adminLessonSegmentService);
        }

        @Test
        @DisplayName("should return 404 LESSON_SEGMENT_NOT_FOUND when segment missing")
        void shouldReturn404_whenNotFound() throws Exception {
            UpdateLessonSegmentRequest request = new UpdateLessonSegmentRequest(
                    1500, 6200, "Updated text", "Văn bản mới");
            when(adminLessonSegmentService.updateSegment(eq(ADMIN_ID), eq(SEGMENT_ID), any(UpdateLessonSegmentRequest.class)))
                    .thenThrow(new AppException(AppError.LESSON_SEGMENT_NOT_FOUND));

            mockMvc.perform(put("/admin/lesson-segments/{segmentId}", SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_SEGMENT_NOT_FOUND"));
        }
    }
}
