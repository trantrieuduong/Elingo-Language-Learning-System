package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.learning.flashcard.dto.response.CardLearningState;
import com.elingo.vocabulary.dto.request.CreateUserCardRequest;
import com.elingo.vocabulary.dto.request.UpdateUserCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.service.CardService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CardService cardService;

    @InjectMocks
    private CardController cardController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long TOPIC_ID = 10L;
    private static final Long CARD_ID = 100L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cardController)
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

    // ─────────────────────── helpers ───────────────────────

    private CardResponse buildCardResponse(String term) {
        return new CardResponse(CARD_ID, 1, term, "n", "nghia", "giai thich",
                "explanation", "vi du", "example", null, List.of(), CardLearningState.NONE);
    }

    private String createBody(String term, String pos, String translation) throws Exception {
        return objectMapper.writeValueAsString(new CreateUserCardRequest(
                TOPIC_ID, term, pos, translation, null, null, null, null));
    }

    private String updateBody(String term, String pos, String translation) throws Exception {
        return objectMapper.writeValueAsString(new UpdateUserCardRequest(
                term, pos, translation, null, null, null, null));
    }

    // ─────────────────────── createUserCard ───────────────────────

    @Nested
    @DisplayName("createUserCard")
    class CreateUserCardTests {

        @Test
        @DisplayName("Create user card successfully")
        void createUserCard_Success() throws Exception {
            when(cardService.createUserCard(eq(USER_ID), any(CreateUserCardRequest.class)))
                    .thenReturn(buildCardResponse("hello"));

            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("hello", "n", "xin chao")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(CARD_ID))
                    .andExpect(jsonPath("$.data.term").value("hello"))
                    .andExpect(jsonPath("$.data.translation").value("nghia"));

            ArgumentCaptor<CreateUserCardRequest> captor = ArgumentCaptor.forClass(CreateUserCardRequest.class);
            verify(cardService).createUserCard(eq(USER_ID), captor.capture());
            assertThat(captor.getValue().topicId()).isEqualTo(TOPIC_ID);
            assertThat(captor.getValue().term()).isEqualTo("hello");
            assertThat(captor.getValue().pos()).isEqualTo("n");
            assertThat(captor.getValue().translation()).isEqualTo("xin chao");
        }

        @Test
        @DisplayName("Create user card successfully: trims text and lowercases pos")
        void createUserCard_Success_NormalizesInput() throws Exception {
            when(cardService.createUserCard(eq(USER_ID), any(CreateUserCardRequest.class)))
                    .thenReturn(buildCardResponse("hello"));

            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("  hello  ", "  Noun  ", "  xin chao  ")))
                    .andExpect(status().isOk());

            ArgumentCaptor<CreateUserCardRequest> captor = ArgumentCaptor.forClass(CreateUserCardRequest.class);
            verify(cardService).createUserCard(eq(USER_ID), captor.capture());
            assertThat(captor.getValue().term()).isEqualTo("hello");
            assertThat(captor.getValue().pos()).isEqualTo("noun");
            assertThat(captor.getValue().translation()).isEqualTo("xin chao");
        }

        @Test
        @DisplayName("Create user card successfully: blank optional fields become null")
        void createUserCard_Success_BlankOptionalFields() throws Exception {
            when(cardService.createUserCard(eq(USER_ID), any(CreateUserCardRequest.class)))
                    .thenReturn(buildCardResponse("hello"));

            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicId\":10,\"term\":\"hello\",\"translation\":\"xin chao\","
                                    + "\"pos\":\"   \",\"explanationVi\":\"   \",\"explanationEn\":\"   \","
                                    + "\"examplesVi\":\"   \",\"examplesEn\":\"   \"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<CreateUserCardRequest> captor = ArgumentCaptor.forClass(CreateUserCardRequest.class);
            verify(cardService).createUserCard(eq(USER_ID), captor.capture());
            CreateUserCardRequest captured = captor.getValue();
            assertThat(captured.pos()).isNull();
            assertThat(captured.explanationVi()).isNull();
            assertThat(captured.explanationEn()).isNull();
            assertThat(captured.examplesVi()).isNull();
            assertThat(captured.examplesEn()).isNull();
        }

        @Test
        @DisplayName("Create user card: Fail because term is blank")
        void createUserCard_Fail_BlankTerm() throws Exception {
            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("   ", "n", "xin chao")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(cardService, never()).createUserCard(any(), any());
        }

        @Test
        @DisplayName("Create user card: Fail because translation is missing")
        void createUserCard_Fail_MissingTranslation() throws Exception {
            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicId\":10,\"term\":\"hello\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(cardService, never()).createUserCard(any(), any());
        }

        @Test
        @DisplayName("Create user card: Fail because topicId is missing")
        void createUserCard_Fail_MissingTopicId() throws Exception {
            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"term\":\"hello\",\"translation\":\"xin chao\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(cardService, never()).createUserCard(any(), any());
        }

        @Test
        @DisplayName("Create user card: Fail because topicId is not positive")
        void createUserCard_Fail_NonPositiveTopicId() throws Exception {
            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateUserCardRequest(
                                    0L, "hello", "n", "xin chao", null, null, null, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(cardService, never()).createUserCard(any(), any());
        }

        @Test
        @DisplayName("Create user card: Fail because term is too long")
        void createUserCard_Fail_TermTooLong() throws Exception {
            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("x".repeat(101), "n", "xin chao")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(cardService, never()).createUserCard(any(), any());
        }

        @Test
        @DisplayName("Create user card: Fail because translation is too long")
        void createUserCard_Fail_TranslationTooLong() throws Exception {
            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("hello", "n", "x".repeat(501))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(cardService, never()).createUserCard(any(), any());
        }

        @Test
        @DisplayName("Create user card: Fail because pos is too long")
        void createUserCard_Fail_PosTooLong() throws Exception {
            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("hello", "x".repeat(51), "xin chao")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(cardService, never()).createUserCard(any(), any());
        }

        @Test
        @DisplayName("Create user card: Fail because topic is not found")
        void createUserCard_Fail_TopicNotFound() throws Exception {
            when(cardService.createUserCard(eq(USER_ID), any(CreateUserCardRequest.class)))
                    .thenThrow(new AppException(AppError.TOPIC_NOT_FOUND));

            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("hello", "n", "xin chao")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }

        @Test
        @DisplayName("Create user card: Fail because user does not own the deck")
        void createUserCard_Fail_AccessDenied() throws Exception {
            when(cardService.createUserCard(eq(USER_ID), any(CreateUserCardRequest.class)))
                    .thenThrow(new AppException(AppError.DECK_ACCESS_DENIED));

            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("hello", "n", "xin chao")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("Create user card: Fail because card limit is exceeded")
        void createUserCard_Fail_CardLimitExceeded() throws Exception {
            when(cardService.createUserCard(eq(USER_ID), any(CreateUserCardRequest.class)))
                    .thenThrow(new AppException(AppError.CARD_LIMIT_EXCEEDED));

            mockMvc.perform(post("/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody("hello", "n", "xin chao")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("CARD_LIMIT_EXCEEDED"));
        }
    }

    // ─────────────────────── updateUserCard ───────────────────────

    @Nested
    @DisplayName("updateUserCard")
    class UpdateUserCardTests {

        @Test
        @DisplayName("Update user card successfully")
        void updateUserCard_Success() throws Exception {
            when(cardService.updateUserCard(eq(USER_ID), eq(CARD_ID), any(UpdateUserCardRequest.class)))
                    .thenReturn(buildCardResponse("hello"));

            mockMvc.perform(put("/cards/{cardId}", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody("hello", "n", "xin chao")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.term").value("hello"));

            ArgumentCaptor<UpdateUserCardRequest> captor = ArgumentCaptor.forClass(UpdateUserCardRequest.class);
            verify(cardService).updateUserCard(eq(USER_ID), eq(CARD_ID), captor.capture());
            assertThat(captor.getValue().term()).isEqualTo("hello");
            assertThat(captor.getValue().translation()).isEqualTo("xin chao");
        }

        @Test
        @DisplayName("Update user card: Fail because term is blank")
        void updateUserCard_Fail_BlankTerm() throws Exception {
            mockMvc.perform(put("/cards/{cardId}", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody("  ", "n", "xin chao")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(cardService, never()).updateUserCard(any(), any(), any());
        }

        @Test
        @DisplayName("Update user card: Fail because card is not found")
        void updateUserCard_Fail_CardNotFound() throws Exception {
            when(cardService.updateUserCard(eq(USER_ID), eq(CARD_ID), any(UpdateUserCardRequest.class)))
                    .thenThrow(new AppException(AppError.CARD_NOT_FOUND));

            mockMvc.perform(put("/cards/{cardId}", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody("hello", "n", "xin chao")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("CARD_NOT_FOUND"));
        }

        @Test
        @DisplayName("Update user card: Fail because user does not own the card")
        void updateUserCard_Fail_AccessDenied() throws Exception {
            when(cardService.updateUserCard(eq(USER_ID), eq(CARD_ID), any(UpdateUserCardRequest.class)))
                    .thenThrow(new AppException(AppError.DECK_ACCESS_DENIED));

            mockMvc.perform(put("/cards/{cardId}", CARD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody("hello", "n", "xin chao")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }
    }

    // ─────────────────────── deleteUserCard ───────────────────────

    @Nested
    @DisplayName("deleteUserCard")
    class DeleteUserCardTests {

        @Test
        @DisplayName("Delete user card successfully")
        void deleteUserCard_Success() throws Exception {
            mockMvc.perform(delete("/cards/{cardId}", CARD_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(cardService).deleteUserCard(USER_ID, CARD_ID);
        }

        @Test
        @DisplayName("Delete user card: Fail because card is not found")
        void deleteUserCard_Fail_CardNotFound() throws Exception {
            doThrow(new AppException(AppError.CARD_NOT_FOUND))
                    .when(cardService).deleteUserCard(USER_ID, CARD_ID);

            mockMvc.perform(delete("/cards/{cardId}", CARD_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("CARD_NOT_FOUND"));
        }

        @Test
        @DisplayName("Delete user card: Fail because user does not own the card")
        void deleteUserCard_Fail_AccessDenied() throws Exception {
            doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(cardService).deleteUserCard(USER_ID, CARD_ID);

            mockMvc.perform(delete("/cards/{cardId}", CARD_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }
    }
}
