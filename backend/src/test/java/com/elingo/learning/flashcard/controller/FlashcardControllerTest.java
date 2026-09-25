package com.elingo.learning.flashcard.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.learning.flashcard.dto.request.SrsReviewRequest;
import com.elingo.learning.flashcard.dto.response.QuizOptionResponse;
import com.elingo.learning.flashcard.dto.response.QuizResponse;
import com.elingo.learning.flashcard.service.FlashcardService;
import com.elingo.vocabulary.dto.response.CardResponse;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
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
    private static final Long TOPIC_ID = 10L;
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
    @DisplayName("getCardsByTopic")
    class GetCardsByTopicTests {

        @Test
        @DisplayName("Get cards by topic successfully")
        void getCardsByTopic_Success() throws Exception {
            CardResponse card1 = new CardResponse(CARD_ID, 1, "hello", "n", "xin chao",
                    "Loi chao thong dung", "A common greeting", "Xin chao, ban!", "Hello, friend!", null, List.of());
            CardResponse card2 = new CardResponse(CARD_ID + 1, 2, "world", "n", "the gioi",
                    "Hanh tinh chung ta", "Our planet", "The gioi that dep!", "The world is beautiful!", null, List.of());

            when(flashcardService.getCardsByTopic(USER_ID, TOPIC_ID)).thenReturn(List.of(card1, card2));

            mockMvc.perform(get("/flashcard/topics/{topicId}/cards", TOPIC_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].term").value("hello"))
                    .andExpect(jsonPath("$.data[1].term").value("world"));

            verify(flashcardService).getCardsByTopic(USER_ID, TOPIC_ID);
            // Kiểm tra xem phương thức getCardsByTopic được gọi đúng 1 lần
        }

        @Test
        @DisplayName("Get cards by topic - returns empty list when no cards found")
        void getCardsByTopic_EmptyList() throws Exception {
            when(flashcardService.getCardsByTopic(USER_ID, TOPIC_ID)).thenReturn(List.of());

            mockMvc.perform(get("/flashcard/topics/{topicId}/cards", TOPIC_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(flashcardService).getCardsByTopic(USER_ID, TOPIC_ID);
        }
    }

    @Nested
    @DisplayName("getQuizOptions")
    class GetQuizOptionsTests {

        @Test
        @DisplayName("Get quiz options successfully")
        void getQuizOptions_Success() throws Exception {
            List<QuizOptionResponse> options = List.of(
                    new QuizOptionResponse("hi", true),
                    new QuizOptionResponse("bye", false),
                    new QuizOptionResponse("thank", false),
                    new QuizOptionResponse("sorry", false)
            );
            QuizResponse quizResponse = new QuizResponse(options);

            when(flashcardService.getQuizOptions(USER_ID, TOPIC_ID, CARD_ID)).thenReturn(quizResponse);

            mockMvc.perform(get("/flashcard/topics/{topicId}/cards/{cardId}/quiz-options", TOPIC_ID, CARD_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.options").isArray())
                    .andExpect(jsonPath("$.data.options.length()").value(4))
                    .andExpect(jsonPath("$.data.options[0].term").value("hi"))
                    .andExpect(jsonPath("$.data.options[0].isCorrect").value(true));

            verify(flashcardService).getQuizOptions(USER_ID, TOPIC_ID, CARD_ID);
        }
    }

    @Nested
    @DisplayName("submitSrsReview")
    class SubmitSrsReviewTests {

        @Test
        @DisplayName("Submit SRS review with grade AGAIN successfully")
        void submitSrsReview_GradeAgain_Success() throws Exception {
            SrsReviewRequest request = new SrsReviewRequest(0);

            mockMvc.perform(patch("/flashcard/{cardId}/srs", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(flashcardService).submitSrsReview(eq(USER_ID), eq(CARD_ID), eq(request));
        }

        @Test
        @DisplayName("Submit SRS review with grade HARD successfully")
        void submitSrsReview_GradeHard_Success() throws Exception {
            SrsReviewRequest request = new SrsReviewRequest(1);

            mockMvc.perform(patch("/flashcard/{cardId}/srs", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(flashcardService).submitSrsReview(eq(USER_ID), eq(CARD_ID), eq(request));
        }

        @Test
        @DisplayName("Submit SRS review with grade GOOD successfully")
        void submitSrsReview_GradeGood_Success() throws Exception {
            SrsReviewRequest request = new SrsReviewRequest(2);

            mockMvc.perform(patch("/flashcard/{cardId}/srs", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(flashcardService).submitSrsReview(eq(USER_ID), eq(CARD_ID), eq(request));
        }

        @Test
        @DisplayName("Submit SRS review with grade EASY successfully")
        void submitSrsReview_GradeEasy_Success() throws Exception {
            SrsReviewRequest request = new SrsReviewRequest(3);

            mockMvc.perform(patch("/flashcard/{cardId}/srs", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(flashcardService).submitSrsReview(eq(USER_ID), eq(CARD_ID), eq(request));
        }
    }
}
