package com.elingo.learning.dictation.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.learning.dictation.dto.request.UpdateDictationProgressRequest;
import com.elingo.learning.dictation.service.DictationService;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.dto.response.SegmentSubmitResponse;
import com.elingo.learning.lesson.entity.ProgressStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class DictationControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DictationService dictationService;

    @InjectMocks
    private DictationController dictationController;

    private static final Long USER_ID = 1L;
    private static final Long LESSON_ID = 10L;
    private static final Long SEGMENT_ID = 100L;
    private static final String URL = "/lessons/{lessonId}/segments/{segmentId}/dictation-progress";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dictationController)
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
                        return USER_ID;
                    }
                })
                .build();
    }

    private SegmentSubmitResponse buildSubmitResponse(int score, boolean passed) {
        SegmentProgressResponse segmentProgress = new SegmentProgressResponse(
                SEGMENT_ID, 1, new BigDecimal(score), 0, 0, BigDecimal.ZERO, null, List.of());
        LessonProgressSummaryResponse lessonProgress = new LessonProgressSummaryResponse(
                ProgressStatus.IN_PROGRESS, new BigDecimal("25.00"), 3000,
                ProgressStatus.NOT_STARTED, BigDecimal.ZERO, null);
        return new SegmentSubmitResponse(score, passed, segmentProgress, lessonProgress);
    }

    // ========================================================================
    // updateProgress
    // ========================================================================
    @Nested
    @DisplayName("updateProgress")
    class UpdateProgressTests {

        @Test
        @DisplayName("Submit dictation result successfully")
        void updateProgress_Success() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(2, 1);
            when(dictationService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .thenReturn(buildSubmitResponse(85, true));

            mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.score").value(85))
                    .andExpect(jsonPath("$.data.passed").value(true))
                    .andExpect(jsonPath("$.data.segmentProgress.segmentId").value(SEGMENT_ID))
                    .andExpect(jsonPath("$.data.segmentProgress.dictationBestScore").value(85))
                    .andExpect(jsonPath("$.data.lessonProgress.dictationStatus").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.data.lessonProgress.dictationProgressPct").value(25.0));

            verify(dictationService).updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);
        }

        @Test
        @DisplayName("Submit dictation result successfully but not passed")
        void updateProgress_Success_NotPassed() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(5, 10);
            when(dictationService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .thenReturn(buildSubmitResponse(20, false));

            mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.score").value(20))
                    .andExpect(jsonPath("$.data.passed").value(false));
        }

        @Test
        @DisplayName("Submit dictation result successfully with boundary values")
        void updateProgress_Success_BoundaryValues() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(1000, 10000);
            when(dictationService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .thenReturn(buildSubmitResponse(0, false));

            mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Submit dictation result failed: Attempt count is missing")
        void updateProgress_Fail_AttemptCountMissing() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(null, 0);

            mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("ATTEMPT_COUNT_INVALID"));

            verify(dictationService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit dictation result failed: Attempt count is less than 1")
        void updateProgress_Fail_AttemptCountTooSmall() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(0, 0);

            mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("ATTEMPT_COUNT_INVALID"));

            verify(dictationService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit dictation result failed: Attempt count is greater than 1000")
        void updateProgress_Fail_AttemptCountTooLarge() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(1001, 0);

            mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("ATTEMPT_COUNT_INVALID"));

            verify(dictationService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit dictation result failed: Hint used count is missing")
        void updateProgress_Fail_HintCountMissing() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(1, null);

            mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("HINT_COUNT_INVALID"));

            verify(dictationService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit dictation result failed: Hint used count is negative")
        void updateProgress_Fail_HintCountNegative() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(1, -1);

            mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("HINT_COUNT_INVALID"));

            verify(dictationService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit dictation result failed: Hint used count is greater than 10000")
        void updateProgress_Fail_HintCountTooLarge() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(1, 10001);

            mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("HINT_COUNT_INVALID"));

            verify(dictationService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit dictation result failed: Lesson id is not positive")
        void updateProgress_Fail_InvalidLessonId() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(1, 0);

            mockMvc.perform(patch(URL, -1, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(dictationService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit dictation result failed: Lesson not found")
        void updateProgress_Fail_LessonNotFound() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(1, 0);
            when(dictationService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .thenThrow(new AppException(AppError.LESSON_NOT_FOUND));

            mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_NOT_FOUND"));
        }

        @Test
        @DisplayName("Submit dictation result failed: Segment not found")
        void updateProgress_Fail_SegmentNotFound() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(1, 0);
            when(dictationService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .thenThrow(new AppException(AppError.LESSON_SEGMENT_NOT_FOUND));

            mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("LESSON_SEGMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("Submit dictation result failed: Premium required")
        void updateProgress_Fail_PremiumRequired() throws Exception {
            UpdateDictationProgressRequest request = new UpdateDictationProgressRequest(1, 0);
            when(dictationService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .thenThrow(new AppException(AppError.PREMIUM_REQUIRED));

            mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0].code").value("PREMIUM_REQUIRED"));
        }
    }
}
