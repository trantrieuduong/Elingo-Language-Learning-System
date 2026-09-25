package com.elingo.learning.flashcard.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.service.FlashcardService;
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

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class FlashcardControllerTest {
    private MockMvc mockMvc;

    @Mock
    private FlashcardService flashcardService;

    @InjectMocks
    private FlashcardController flashcardController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 100L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(flashcardController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(@NonNull MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(CurrentUserId.class);
                    }

                    @Override
                    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  @NonNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return USER_ID;
                    }
                })
                .build();
    }


    @Nested
    @DisplayName("submitSrsReview")
    class SubmitSrsReviewTests {

        @Test
        @DisplayName("Submit SRS review with grade AGAIN successfully")
        void submitSrsReview_GradeAgain_Success() throws Exception {
            SrsReviewRequest request = new SrsReviewRequest(0);

            mockMvc.perform(patch("/flashcards/{cardId}/srs", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(flashcardService).submitSrsReview(USER_ID, CARD_ID, request);
        }

        @Test
        @DisplayName("Submit SRS review with grade HARD successfully")
        void submitSrsReview_GradeHard_Success() throws Exception {
            SrsReviewRequest request = new SrsReviewRequest(1);

            mockMvc.perform(patch("/flashcards/{cardId}/srs", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(flashcardService).submitSrsReview(USER_ID, CARD_ID, request);
        }

        @Test
        @DisplayName("Submit SRS review with grade GOOD successfully")
        void submitSrsReview_GradeGood_Success() throws Exception {
            SrsReviewRequest request = new SrsReviewRequest(2);

            mockMvc.perform(patch("/flashcards/{cardId}/srs", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(flashcardService).submitSrsReview(USER_ID, CARD_ID, request);
        }

        @Test
        @DisplayName("Submit SRS review with grade EASY successfully")
        void submitSrsReview_GradeEasy_Success() throws Exception {
            SrsReviewRequest request = new SrsReviewRequest(3);

            mockMvc.perform(patch("/flashcards/{cardId}/srs", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(flashcardService).submitSrsReview(USER_ID, CARD_ID, request);
        }
    }
}
