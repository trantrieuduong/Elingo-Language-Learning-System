package com.elingo.learning.flashcard.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.learning.flashcard.service.CardStateService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class FlashcardControllerTest {
    private MockMvc mockMvc;

    @Mock
    private CardStateService cardStateService;

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

            mockMvc.perform(post("/flashcards/{cardId}/srs", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(cardStateService).submitReview(USER_ID, CARD_ID, request);
        }

        @Test
        @DisplayName("Submit SRS review with grade HARD successfully")
        void submitSrsReview_GradeHard_Success() throws Exception {
            SrsReviewRequest request = new SrsReviewRequest(1);

            mockMvc.perform(post("/flashcards/{cardId}/srs", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(cardStateService).submitReview(USER_ID, CARD_ID, request);
        }

        @Test
        @DisplayName("Submit SRS review with grade GOOD successfully")
        void submitSrsReview_GradeGood_Success() throws Exception {
            SrsReviewRequest request = new SrsReviewRequest(2);

            mockMvc.perform(post("/flashcards/{cardId}/srs", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(cardStateService).submitReview(USER_ID, CARD_ID, request);
        }

        @Test
        @DisplayName("Submit SRS review with grade EASY successfully")
        void submitSrsReview_GradeEasy_Success() throws Exception {
            SrsReviewRequest request = new SrsReviewRequest(3);

            mockMvc.perform(post("/flashcards/{cardId}/srs", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(cardStateService).submitReview(USER_ID, CARD_ID, request);
        }
    }

    @Nested
    @DisplayName("toggleStar")
    class ToggleStarTests {

        @Test
        @DisplayName("Toggle star state successfully")
        void toggleStar_Success() throws Exception {
            mockMvc.perform(put("/flashcards/stars")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cardId\": " + CARD_ID + ", \"starred\": true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(cardStateService).setStarred(USER_ID, CARD_ID, true);
        }
    }

    @Nested
    @DisplayName("toggleHidden")
    class ToggleHiddenTests {

        @Test
        @DisplayName("Toggle hidden state successfully")
        void toggleHidden_Success() throws Exception {
            mockMvc.perform(put("/flashcards/hidden")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cardId\": " + CARD_ID + ", \"hidden\": true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(cardStateService).setHidden(USER_ID, CARD_ID, true);
        }
    }

    @Nested
    @DisplayName("getDueCards")
    class GetDueCardsTests {

        @Test
        @DisplayName("Get list of flashcards due for review successfully")
        void getDueCards_Success() throws Exception {
            CardResponse cardResponse = new CardResponse(
                    CARD_ID, 1, "hello", "verb", "chào",
                    "chào hỏi người khác", "greeting in English",
                    "chào mọi người", "hello everyone",
                    null, List.of(), null
            );
            when(cardStateService.getDueCards(USER_ID, 100)).thenReturn(List.of(cardResponse));

            mockMvc.perform(get("/flashcards/due")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(CARD_ID))
                    .andExpect(jsonPath("$.data[0].term").value("hello"));

            verify(cardStateService).getDueCards(USER_ID, 100);
        }

        @Test
        @DisplayName("Get due cards with custom limit")
        void getDueCards_CustomLimit() throws Exception {
            when(cardStateService.getDueCards(USER_ID, 50)).thenReturn(List.of());

            mockMvc.perform(get("/flashcards/due").param("limit", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(cardStateService).getDueCards(USER_ID, 50);
        }
    }
}