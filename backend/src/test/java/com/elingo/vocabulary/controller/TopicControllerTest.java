package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.learning.flashcard.dto.response.QuizOptionResponse;
import com.elingo.vocabulary.service.TopicService;
import com.elingo.vocabulary.dto.response.CardResponse;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class TopicControllerTest {
    private MockMvc mockMvc;

    @Mock
    private TopicService topicService;

    @InjectMocks
    private TopicController topicController;

    private static final Long USER_ID = 1L;
    private static final Long TOPIC_ID = 10L;
    private static final Long CARD_ID = 100L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(topicController)
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

            when(topicService.getCardsByTopic(USER_ID, TOPIC_ID)).thenReturn(List.of(card1, card2));

            mockMvc.perform(get("/topics/{topicId}/cards", TOPIC_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].term").value("hello"))
                    .andExpect(jsonPath("$.data[1].term").value("world"));

            verify(topicService).getCardsByTopic(USER_ID, TOPIC_ID);
        }

        @Test
        @DisplayName("Get cards by topic - returns empty list when no cards found")
        void getCardsByTopic_EmptyList() throws Exception {
            when(topicService.getCardsByTopic(USER_ID, TOPIC_ID)).thenReturn(List.of());

            mockMvc.perform(get("/topics/{topicId}/cards", TOPIC_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(topicService).getCardsByTopic(USER_ID, TOPIC_ID);
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
            when(topicService.getQuizOptions(USER_ID, TOPIC_ID, CARD_ID)).thenReturn(options);

            mockMvc.perform(get("/topics/{topicId}/cards/{cardId}/quiz-options", TOPIC_ID, CARD_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(4))
                    .andExpect(jsonPath("$.data[0].term").value("hi"))
                    .andExpect(jsonPath("$.data[0].isCorrect").value(true));

            verify(topicService).getQuizOptions(USER_ID, TOPIC_ID, CARD_ID);
        }
    }
}
