package com.elingo.learning.shadowing.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.dto.response.SegmentSubmitResponse;
import com.elingo.learning.lesson.dto.response.WordAccuracyResponse;
import com.elingo.learning.lesson.entity.ProgressStatus;
import com.elingo.learning.shadowing.dto.request.UpdateShadowingProgressRequest;
import com.elingo.learning.shadowing.dto.request.WordAccuracyItem;
import com.elingo.learning.shadowing.service.ShadowingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ShadowingControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ShadowingService shadowingService;

    @InjectMocks
    private ShadowingController shadowingController;

    private static final Long USER_ID = 1L;
    private static final Long LESSON_ID = 10L;
    private static final Long SEGMENT_ID = 100L;
    private static final String URL = "/lessons/{lessonId}/segments/{segmentId}/shadowing-progress";
    private static final String AUDIO_KEY = "verified/1/recording.mp3";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shadowingController)
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

    private List<WordAccuracyItem> validWords() {
        return List.of(
                new WordAccuracyItem("hello", new BigDecimal("95.50")),
                new WordAccuracyItem("world", new BigDecimal("60")));
    }

    private UpdateShadowingProgressRequest validRequest() {
        return new UpdateShadowingProgressRequest(1, 80, validWords(), AUDIO_KEY);
    }

    private SegmentSubmitResponse buildSubmitResponse(int score, boolean passed) {
        SegmentProgressResponse segmentProgress = new SegmentProgressResponse(
                SEGMENT_ID, 0, BigDecimal.ZERO, 0, 1, new BigDecimal(score), null,
                List.of(new WordAccuracyResponse("hello", new BigDecimal("95.50"))));
        LessonProgressSummaryResponse lessonProgress = new LessonProgressSummaryResponse(
                ProgressStatus.NOT_STARTED, BigDecimal.ZERO, null,
                ProgressStatus.IN_PROGRESS, new BigDecimal("25.00"), 3000);
        return new SegmentSubmitResponse(score, passed, segmentProgress, lessonProgress);
    }

    /**
     * Gửi request với audioUrl = null. Trường audioUrl dùng @BuildMediaKey nên khi có giá trị sẽ đi qua
     * MediaKeyDeserializer (cần cấu hình r2.public-url của Spring) - không dùng được trong standalone MockMvc.
     * Ngoài ra controller có @Positive trên path variable nên Spring dùng method validation: GlobalExceptionHandler
     * chỉ trả về MỘT mã lỗi (mã đầu tiên, không đảm bảo thứ tự) khi có nhiều vi phạm. Vì vậy các test MockMvc bên dưới
     * chỉ kiểm tra 400 + service không được gọi; mã lỗi của từng trường được kiểm tra ở nhóm "request validation".
     */
    private org.springframework.test.web.servlet.ResultActions performWithoutAudio(
            Integer attemptCount, Integer score, List<WordAccuracyItem> words) throws Exception {
        UpdateShadowingProgressRequest request = new UpdateShadowingProgressRequest(attemptCount, score, words, null);
        return mockMvc.perform(patch(URL, LESSON_ID, SEGMENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    // ========================================================================
    // updateProgress - delegate to service
    // ========================================================================
    @Nested
    @DisplayName("updateProgress")
    class UpdateProgressTests {

        @Test
        @DisplayName("Submit shadowing result successfully")
        void updateProgress_Success() {
            UpdateShadowingProgressRequest request = validRequest();
            when(shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .thenReturn(buildSubmitResponse(80, true));

            // Gọi trực tiếp controller vì request có audioUrl không thể đi qua MockMvc (xem performWithoutAudio)
            ApiResponse<SegmentSubmitResponse> response =
                    shadowingController.updateProgress(LESSON_ID, SEGMENT_ID, request, USER_ID);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().score()).isEqualTo(80);
            assertThat(response.getData().passed()).isTrue();
            assertThat(response.getData().segmentProgress().shadowingBestScore()).isEqualByComparingTo("80");
            assertThat(response.getData().segmentProgress().wordsAccuracy()).hasSize(1);
            assertThat(response.getData().lessonProgress().shadowingStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
            verify(shadowingService).updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request);
        }

        @Test
        @DisplayName("Submit shadowing result successfully but not passed")
        void updateProgress_Success_NotPassed() {
            UpdateShadowingProgressRequest request = validRequest();
            when(shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .thenReturn(buildSubmitResponse(30, false));

            ApiResponse<SegmentSubmitResponse> response =
                    shadowingController.updateProgress(LESSON_ID, SEGMENT_ID, request, USER_ID);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().passed()).isFalse();
        }

        @Test
        @DisplayName("Submit shadowing result failed: Service error is propagated")
        void updateProgress_Fail_ServiceError() {
            UpdateShadowingProgressRequest request = validRequest();
            when(shadowingService.updateProgress(USER_ID, LESSON_ID, SEGMENT_ID, request))
                    .thenThrow(new AppException(AppError.FILE_ACCESS_DENIED));

            assertThatThrownBy(() -> shadowingController.updateProgress(LESSON_ID, SEGMENT_ID, request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_ACCESS_DENIED));
        }
    }

    // ========================================================================
    // updateProgress - validation qua MockMvc
    // ========================================================================
    @Nested
    @DisplayName("updateProgress validation")
    class UpdateProgressValidationTests {

        @Test
        @DisplayName("Submit shadowing result failed: Audio recording is missing")
        void updateProgress_Fail_AudioMissing() throws Exception {
            performWithoutAudio(1, 80, validWords())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("AUDIO_RECORDING_REQUIRED"));

            verify(shadowingService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit shadowing result failed: Attempt count is missing")
        void updateProgress_Fail_AttemptCountMissing() throws Exception {
            performWithoutAudio(null, 80, validWords())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(shadowingService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit shadowing result failed: Attempt count is out of range")
        void updateProgress_Fail_AttemptCountOutOfRange() throws Exception {
            performWithoutAudio(0, 80, validWords())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
            performWithoutAudio(1001, 80, validWords())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(shadowingService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit shadowing result failed: Score is missing")
        void updateProgress_Fail_ScoreMissing() throws Exception {
            performWithoutAudio(1, null, validWords())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(shadowingService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit shadowing result failed: Score is out of range")
        void updateProgress_Fail_ScoreOutOfRange() throws Exception {
            performWithoutAudio(1, -1, validWords())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
            performWithoutAudio(1, 101, validWords())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(shadowingService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit shadowing result failed: Words accuracy list is missing")
        void updateProgress_Fail_WordsAccuracyMissing() throws Exception {
            performWithoutAudio(1, 80, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(shadowingService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit shadowing result failed: Words accuracy list has more than 300 items")
        void updateProgress_Fail_WordsAccuracyTooMany() throws Exception {
            List<WordAccuracyItem> words = new ArrayList<>();
            for (int i = 0; i < 301; i++) {
                words.add(new WordAccuracyItem("w" + i, BigDecimal.TEN));
            }

            performWithoutAudio(1, 80, words)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(shadowingService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit shadowing result failed: A word is blank")
        void updateProgress_Fail_WordBlank() throws Exception {
            performWithoutAudio(1, 80, List.of(new WordAccuracyItem("  ", BigDecimal.TEN)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(shadowingService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit shadowing result failed: A word accuracy is out of range")
        void updateProgress_Fail_WordAccuracyOutOfRange() throws Exception {
            performWithoutAudio(1, 80, List.of(new WordAccuracyItem("hello", new BigDecimal("100.5"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
            performWithoutAudio(1, 80, List.of(new WordAccuracyItem("hello", new BigDecimal("-1"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(shadowingService, never()).updateProgress(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit shadowing result failed: Lesson id is not positive")
        void updateProgress_Fail_InvalidLessonId() throws Exception {
            // Body hợp lệ trừ audioUrl; lỗi path variable được xử lý trước khi validate body
            UpdateShadowingProgressRequest request = new UpdateShadowingProgressRequest(1, 80, validWords(), null);

            mockMvc.perform(patch(URL, -1, SEGMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(shadowingService, never()).updateProgress(any(), any(), any(), any());
        }
    }

    // ========================================================================
    // Request validation (các ràng buộc của audioUrl)
    // ========================================================================
    @Nested
    @DisplayName("request validation")
    class RequestValidationTests {

        private Validator validator;

        @BeforeEach
        void initValidator() {
            validator = Validation.buildDefaultValidatorFactory().getValidator();
        }

        private List<String> violationMessages(UpdateShadowingProgressRequest request) {
            return validator.validate(request).stream().map(ConstraintViolation::getMessage).toList();
        }

        @Test
        @DisplayName("Request is valid with an audio key")
        void request_Valid() {
            assertThat(violationMessages(validRequest())).isEmpty();
        }

        @Test
        @DisplayName("Request is valid with an empty words accuracy list")
        void request_Valid_EmptyWords() {
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 0, List.of(), AUDIO_KEY))).isEmpty();
        }

        @Test
        @DisplayName("Request failed: Audio key is blank")
        void request_Fail_AudioBlank() {
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80, validWords(), "  ")))
                    .containsExactly("AUDIO_RECORDING_REQUIRED");
        }

        @Test
        @DisplayName("Request failed: Audio key is not an audio file")
        void request_Fail_AudioWrongKind() {
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80, validWords(), "verified/1/photo.png")))
                    .containsExactly("INVALID_FILE_TYPE");
        }

        @Test
        @DisplayName("Request failed: Audio key has no extension")
        void request_Fail_AudioNoExtension() {
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80, validWords(), "verified/1/recording")))
                    .containsExactly("INVALID_FILE_TYPE");
        }

        @Test
        @DisplayName("Request failed: Audio key is longer than 255 characters")
        void request_Fail_AudioTooLong() {
            String longKey = "verified/1/" + "a".repeat(250) + ".mp3";

            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80, validWords(), longKey)))
                    .containsExactly("INVALID_REQUEST");
        }

        @Test
        @DisplayName("Request failed: Attempt count is missing or out of range")
        void request_Fail_AttemptCount() {
            assertThat(violationMessages(new UpdateShadowingProgressRequest(null, 80, validWords(), AUDIO_KEY)))
                    .containsExactly("ATTEMPT_COUNT_INVALID");
            assertThat(violationMessages(new UpdateShadowingProgressRequest(0, 80, validWords(), AUDIO_KEY)))
                    .containsExactly("ATTEMPT_COUNT_INVALID");
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1001, 80, validWords(), AUDIO_KEY)))
                    .containsExactly("ATTEMPT_COUNT_INVALID");
        }

        @Test
        @DisplayName("Request is valid with boundary attempt count and score")
        void request_Valid_Boundaries() {
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 0, validWords(), AUDIO_KEY))).isEmpty();
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1000, 100, validWords(), AUDIO_KEY))).isEmpty();
        }

        @Test
        @DisplayName("Request failed: Score is missing or out of range")
        void request_Fail_Score() {
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, null, validWords(), AUDIO_KEY)))
                    .containsExactly("SHADOWING_SCORE_INVALID");
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, -1, validWords(), AUDIO_KEY)))
                    .containsExactly("SHADOWING_SCORE_INVALID");
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 101, validWords(), AUDIO_KEY)))
                    .containsExactly("SHADOWING_SCORE_INVALID");
        }

        @Test
        @DisplayName("Request failed: Words accuracy list is missing")
        void request_Fail_WordsAccuracyMissing() {
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80, null, AUDIO_KEY)))
                    .containsExactly("WORDS_ACCURACY_INVALID");
        }

        @Test
        @DisplayName("Request failed: Words accuracy list has more than 300 items")
        void request_Fail_WordsAccuracyTooMany() {
            List<WordAccuracyItem> words = new ArrayList<>();
            for (int i = 0; i < 301; i++) {
                words.add(new WordAccuracyItem("w" + i, BigDecimal.TEN));
            }

            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80, words, AUDIO_KEY)))
                    .containsExactly("WORDS_ACCURACY_INVALID");
        }

        @Test
        @DisplayName("Request failed: Words accuracy list contains a null item")
        void request_Fail_WordsAccuracyNullItem() {
            List<WordAccuracyItem> words = new ArrayList<>();
            words.add(null);

            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80, words, AUDIO_KEY)))
                    .containsExactly("WORDS_ACCURACY_INVALID");
        }

        @Test
        @DisplayName("Request failed: A word is blank or longer than 100 characters")
        void request_Fail_WordInvalid() {
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80,
                    List.of(new WordAccuracyItem("  ", BigDecimal.TEN)), AUDIO_KEY)))
                    .containsExactly("WORDS_ACCURACY_INVALID");
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80,
                    List.of(new WordAccuracyItem("a".repeat(101), BigDecimal.TEN)), AUDIO_KEY)))
                    .containsExactly("WORDS_ACCURACY_INVALID");
        }

        @Test
        @DisplayName("Request failed: A word accuracy is missing or out of range")
        void request_Fail_WordAccuracyInvalid() {
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80,
                    List.of(new WordAccuracyItem("hello", null)), AUDIO_KEY)))
                    .containsExactly("WORDS_ACCURACY_INVALID");
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80,
                    List.of(new WordAccuracyItem("hello", new BigDecimal("100.5"))), AUDIO_KEY)))
                    .containsExactly("WORDS_ACCURACY_INVALID");
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80,
                    List.of(new WordAccuracyItem("hello", new BigDecimal("-0.5"))), AUDIO_KEY)))
                    .containsExactly("WORDS_ACCURACY_INVALID");
        }

        @Test
        @DisplayName("Request is valid with boundary word accuracy")
        void request_Valid_WordAccuracyBoundaries() {
            assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80,
                    List.of(new WordAccuracyItem("a", BigDecimal.ZERO), new WordAccuracyItem("b", new BigDecimal("100"))),
                    AUDIO_KEY))).isEmpty();
        }

        @Test
        @DisplayName("Request is valid with every supported audio extension")
        void request_Valid_SupportedAudioExtensions() {
            for (String ext : List.of("mp3", "wav", "ogg", "m4a")) {
                assertThat(violationMessages(new UpdateShadowingProgressRequest(1, 80, validWords(), "verified/1/rec." + ext)))
                        .as("extension %s", ext)
                        .isEmpty();
            }
        }
    }
}
