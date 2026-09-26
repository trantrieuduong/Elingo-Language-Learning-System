package com.elingo.learning.flashcard.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.response.ReviewCardResponse;
import com.elingo.learning.flashcard.service.FlashcardService;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
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

    @Nested
    @DisplayName("toggleStar")
    class ToggleStarTests {

        @Test
        @DisplayName("Toggle star state successfully")
        void toggleStar_Success() throws Exception {
            mockMvc.perform(patch("/flashcards/{cardId}/stars", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(flashcardService).toggleStar(USER_ID, CARD_ID);
        }
    }

    @Nested
    @DisplayName("toggleHidden")
    class ToggleHiddenTests {

        @Test
        @DisplayName("Toggle hidden state successfully")
        void toggleHidden_Success() throws Exception {
            mockMvc.perform(patch("/flashcards/{cardId}/hidden", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(flashcardService).toggleHide(USER_ID, CARD_ID);
        }
    }

    @Nested
    @DisplayName("getCardsForReview")
    class GetCardsForReviewTests {

        @Test
        @DisplayName("Get list of flashcards due for review successfully")
        void getCardsForReview_Success() throws Exception {
            CardResponse cardResponse = new CardResponse(
                    CARD_ID, 1, "hello", "verb", "chào",
                    "chào hỏi người khác", "greeting in English",
                    "chào mọi người", "hello everyone",
                    "http://image.url", Collections.emptyList()
            );
            ReviewCardResponse reviewCardResponse = new ReviewCardResponse(
                    cardResponse, LocalDateTime.now(), true, false
            );
            List<ReviewCardResponse> responseMock = List.of(reviewCardResponse);

            when(flashcardService.getCardsForReview(USER_ID)).thenReturn(responseMock);

            mockMvc.perform(get("/flashcards/reviews")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].card.id").value(CARD_ID))
                    .andExpect(jsonPath("$.data[0].card.term").value("hello"))
                    .andExpect(jsonPath("$.data[0].flagsStarred").value(true))
                    .andExpect(jsonPath("$.data[0].flagsHidden").value(false));

            verify(flashcardService).getCardsForReview(USER_ID);
        }
    }
}
