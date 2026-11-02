package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.learning.flashcard.dto.response.CardLearningState;
import com.elingo.vocabulary.dto.request.CreateUserTopicRequest;
import com.elingo.vocabulary.dto.request.UpdateUserTopicRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.service.TopicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long DECK_ID = 5L;
    private static final Long TOPIC_ID = 10L;
    private static final Long CARD_ID = 100L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(topicController)
                .setControllerAdvice(new GlobalExceptionHandler())
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

    // ─────────────────────── helpers ───────────────────────

    private TopicResponse buildTopicResponse(String name) {
        return new TopicResponse(TOPIC_ID, name, name.toLowerCase(), 1, 3, 1);
    }

    private String createBody(Long deckId, String name) throws Exception {
        return objectMapper.writeValueAsString(new CreateUserTopicRequest(deckId, name));
    }

    private String updateBody(String name) throws Exception {
        return objectMapper.writeValueAsString(new UpdateUserTopicRequest(name));
    }

    // ─────────────────────── getCardsByTopic ───────────────────────

    @Nested
    @DisplayName("getCardsByTopic")
    class GetCardsByTopicTests {

        @Test
        @DisplayName("Get cards by topic successfully")
        void getCardsByTopic_Success() throws Exception {
            CardResponse card1 = new CardResponse(
                    CARD_ID, 1, "hello", "n", "xin chao",
                    "Loi chao thong dung", "A common greeting", "Xin chao, ban!", "Hello, friend!",
                    null, List.of(),
                    new CardLearningState(true, false, false)
            );
            CardResponse card2 = new CardResponse(
                    CARD_ID + 1, 2, "world", "n", "the gioi",
                    "Hanh tinh chung ta", "Our planet", "The gioi that dep!", "The world is beautiful!",
                    null, List.of(),
                    new CardLearningState(false, false, false)
            );

            when(topicService.getCardsByTopic(TOPIC_ID, USER_ID)).thenReturn(List.of(card1, card2));

            mockMvc.perform(get("/topics/{topicId}/cards", TOPIC_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].term").value("hello"))
                    .andExpect(jsonPath("$.data[0].state.learned").value(true))
                    .andExpect(jsonPath("$.data[1].term").value("world"))
                    .andExpect(jsonPath("$.data[1].state.learned").value(false));

            verify(topicService).getCardsByTopic(TOPIC_ID, USER_ID);
        }

        @Test
        @DisplayName("Get cards by topic - returns empty list when no cards found")
        void getCardsByTopic_EmptyList() throws Exception {
            when(topicService.getCardsByTopic(TOPIC_ID, USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/topics/{topicId}/cards", TOPIC_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(topicService).getCardsByTopic(TOPIC_ID, USER_ID);
        }

        @Test
        @DisplayName("Get cards by topic: Fail because topic is not found")
        void getCardsByTopic_Fail_TopicNotFound() throws Exception {
            when(topicService.getCardsByTopic(TOPIC_ID, USER_ID))
                    .thenThrow(new AppException(AppError.TOPIC_NOT_FOUND));

            mockMvc.perform(get("/topics/{topicId}/cards", TOPIC_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get cards by topic: Fail because premium is required")
        void getCardsByTopic_Fail_PremiumRequired() throws Exception {
            when(topicService.getCardsByTopic(TOPIC_ID, USER_ID))
                    .thenThrow(new AppException(AppError.PREMIUM_REQUIRED));

            mockMvc.perform(get("/topics/{topicId}/cards", TOPIC_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0].code").value("PREMIUM_REQUIRED"));
        }
    }

    // ─────────────────────── createUserTopic ───────────────────────

    @Nested
    @DisplayName("createUserTopic")
    class CreateUserTopicTests {

        @Test
        @DisplayName("Create user topic successfully")
        void createUserTopic_Success() throws Exception {
            when(topicService.createUserTopic(eq(USER_ID), any(CreateUserTopicRequest.class)))
                    .thenReturn(buildTopicResponse("animals"));

            mockMvc.perform(post("/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(DECK_ID, "animals")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("animals"));

            ArgumentCaptor<CreateUserTopicRequest> captor = ArgumentCaptor.forClass(CreateUserTopicRequest.class);
            verify(topicService).createUserTopic(eq(USER_ID), captor.capture());
            assertThat(captor.getValue().deckId()).isEqualTo(DECK_ID);
            assertThat(captor.getValue().name()).isEqualTo("animals");
        }

        @Test
        @DisplayName("Create user topic successfully: trims the name")
        void createUserTopic_Success_TrimsName() throws Exception {
            when(topicService.createUserTopic(eq(USER_ID), any(CreateUserTopicRequest.class)))
                    .thenReturn(buildTopicResponse("animals"));

            mockMvc.perform(post("/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(DECK_ID, "  animals  ")))
                    .andExpect(status().isOk());

            ArgumentCaptor<CreateUserTopicRequest> captor = ArgumentCaptor.forClass(CreateUserTopicRequest.class);
            verify(topicService).createUserTopic(eq(USER_ID), captor.capture());
            assertThat(captor.getValue().name()).isEqualTo("animals");
        }

        @Test
        @DisplayName("Create user topic: Fail because name is blank")
        void createUserTopic_Fail_BlankName() throws Exception {
            mockMvc.perform(post("/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(DECK_ID, "   ")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(topicService, never()).createUserTopic(any(), any());
        }

        @Test
        @DisplayName("Create user topic: Fail because deckId is missing")
        void createUserTopic_Fail_MissingDeckId() throws Exception {
            mockMvc.perform(post("/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"animals\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(topicService, never()).createUserTopic(any(), any());
        }

        @Test
        @DisplayName("Create user topic: Fail because name is too long")
        void createUserTopic_Fail_NameTooLong() throws Exception {
            mockMvc.perform(post("/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(DECK_ID, "x".repeat(51))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(topicService, never()).createUserTopic(any(), any());
        }

        @Test
        @DisplayName("Create user topic: Fail because deck is not found")
        void createUserTopic_Fail_DeckNotFound() throws Exception {
            when(topicService.createUserTopic(eq(USER_ID), any(CreateUserTopicRequest.class)))
                    .thenThrow(new AppException(AppError.DECK_NOT_FOUND));

            mockMvc.perform(post("/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(DECK_ID, "animals")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Create user topic: Fail because user does not own the deck")
        void createUserTopic_Fail_AccessDenied() throws Exception {
            when(topicService.createUserTopic(eq(USER_ID), any(CreateUserTopicRequest.class)))
                    .thenThrow(new AppException(AppError.DECK_ACCESS_DENIED));

            mockMvc.perform(post("/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(DECK_ID, "animals")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Create user topic: Fail because topic limit is exceeded")
        void createUserTopic_Fail_TopicLimitExceeded() throws Exception {
            when(topicService.createUserTopic(eq(USER_ID), any(CreateUserTopicRequest.class)))
                    .thenThrow(new AppException(AppError.TOPIC_LIMIT_EXCEEDED));

            mockMvc.perform(post("/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody(DECK_ID, "animals")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_LIMIT_EXCEEDED"));
        }
    }

    // ─────────────────────── updateUserTopic ───────────────────────

    @Nested
    @DisplayName("updateUserTopic")
    class UpdateUserTopicTests {

        @Test
        @DisplayName("Update user topic successfully")
        void updateUserTopic_Success() throws Exception {
            when(topicService.updateUserTopic(eq(USER_ID), eq(TOPIC_ID), any(UpdateUserTopicRequest.class)))
                    .thenReturn(buildTopicResponse("animals"));

            mockMvc.perform(put("/topics/{topicId}", TOPIC_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody("animals")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("animals"));

            verify(topicService).updateUserTopic(eq(USER_ID), eq(TOPIC_ID), any(UpdateUserTopicRequest.class));
        }

        @Test
        @DisplayName("Update user topic: Fail because name is blank")
        void updateUserTopic_Fail_BlankName() throws Exception {
            mockMvc.perform(put("/topics/{topicId}", TOPIC_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody("")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(topicService, never()).updateUserTopic(any(), any(), any());
        }

        @Test
        @DisplayName("Update user topic: Fail because topic is not found")
        void updateUserTopic_Fail_TopicNotFound() throws Exception {
            when(topicService.updateUserTopic(eq(USER_ID), eq(TOPIC_ID), any(UpdateUserTopicRequest.class)))
                    .thenThrow(new AppException(AppError.TOPIC_NOT_FOUND));

            mockMvc.perform(put("/topics/{topicId}", TOPIC_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody("animals")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }

        @Test
        @DisplayName("Update user topic: Fail because user does not own the deck")
        void updateUserTopic_Fail_AccessDenied() throws Exception {
            when(topicService.updateUserTopic(eq(USER_ID), eq(TOPIC_ID), any(UpdateUserTopicRequest.class)))
                    .thenThrow(new AppException(AppError.DECK_ACCESS_DENIED));

            mockMvc.perform(put("/topics/{topicId}", TOPIC_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody("animals")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }
    }

    // ─────────────────────── deleteUserTopic ───────────────────────

    @Nested
    @DisplayName("deleteUserTopic")
    class DeleteUserTopicTests {

        @Test
        @DisplayName("Delete user topic successfully")
        void deleteUserTopic_Success() throws Exception {
            mockMvc.perform(delete("/topics/{topicId}", TOPIC_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(topicService).deleteUserTopic(USER_ID, TOPIC_ID);
        }

        @Test
        @DisplayName("Delete user topic: Fail because topic is not found")
        void deleteUserTopic_Fail_TopicNotFound() throws Exception {
            doThrow(new AppException(AppError.TOPIC_NOT_FOUND))
                    .when(topicService).deleteUserTopic(USER_ID, TOPIC_ID);

            mockMvc.perform(delete("/topics/{topicId}", TOPIC_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }

        @Test
        @DisplayName("Delete user topic: Fail because user does not own the deck")
        void deleteUserTopic_Fail_AccessDenied() throws Exception {
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(topicService).deleteUserTopic(USER_ID, TOPIC_ID);

            mockMvc.perform(delete("/topics/{topicId}", TOPIC_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }
    }
}
