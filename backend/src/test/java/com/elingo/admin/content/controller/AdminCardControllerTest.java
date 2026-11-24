package com.elingo.admin.content.controller;

import com.elingo.vocabulary.dto.request.CreateSystemCardRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemCardRequest;
import com.elingo.admin.content.service.AdminCardService;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.vocabulary.dto.response.CardResponse;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminCardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminCardService adminCardService;

    @InjectMocks
    private AdminCardController adminCardController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminCardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private CardResponse sampleCardResponse(Long id, String term) {
        return new CardResponse(
                id,
                1,
                term,
                "noun",
                "nghĩa",
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null
        );
    }

    @Nested
    @DisplayName("getCardById")
    class GetCardByIdTests {

        @Test
        @DisplayName("Get card by ID successfully")
        void getCardById_Success() throws Exception {
            when(adminCardService.getCardById(50L)).thenReturn(sampleCardResponse(50L, "term"));

            mockMvc.perform(get("/admin/cards/{cardId}", 50L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(50L))
                    .andExpect(jsonPath("$.data.term").value("term"));

            verify(adminCardService).getCardById(50L);
        }

        @Test
        @DisplayName("Get card by ID fails when card not found")
        void getCardById_NotFound() throws Exception {
            when(adminCardService.getCardById(99L)).thenThrow(new AppException(AppError.CARD_NOT_FOUND));

            mockMvc.perform(get("/admin/cards/{cardId}", 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("CARD_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("createSystemCard")
    class CreateSystemCardTests {

        @Test
        @DisplayName("Create system card successfully")
        void createSystemCard_Success() throws Exception {
            CreateSystemCardRequest request = new CreateSystemCardRequest(
                    10L, "apple", "n", "quả táo", null, null, null, null, null, null
            );
            when(adminCardService.createSystemCard(any(CreateSystemCardRequest.class)))
                    .thenReturn(sampleCardResponse(100L, "apple"));

            mockMvc.perform(post("/admin/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(100L))
                    .andExpect(jsonPath("$.data.term").value("apple"));

            verify(adminCardService).createSystemCard(any(CreateSystemCardRequest.class));
        }

        @Test
        @DisplayName("Create system card fails validation when term is missing")
        void createSystemCard_Validation_MissingTerm() throws Exception {
            CreateSystemCardRequest request = new CreateSystemCardRequest(
                    10L, "", "n", "quả táo", null, null, null, null, null, null
            );

            mockMvc.perform(post("/admin/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(adminCardService, never()).createSystemCard(any());
        }
    }

    @Nested
    @DisplayName("updateSystemCard")
    class UpdateSystemCardTests {

        @Test
        @DisplayName("Update system card successfully")
        void updateSystemCard_Success() throws Exception {
            UpdateSystemCardRequest request = new UpdateSystemCardRequest(
                    "apple", "n", "quả táo đỏ", null, null, null, null, null, null
            );
            when(adminCardService.updateSystemCard(eq(100L), any(UpdateSystemCardRequest.class)))
                    .thenReturn(sampleCardResponse(100L, "apple"));

            mockMvc.perform(put("/admin/cards/{cardId}", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(100L));

            verify(adminCardService).updateSystemCard(eq(100L), any(UpdateSystemCardRequest.class));
        }
    }

    @Nested
    @DisplayName("deleteSystemCard")
    class DeleteSystemCardTests {

        @Test
        @DisplayName("Delete system card successfully")
        void deleteSystemCard_Success() throws Exception {
            mockMvc.perform(delete("/admin/cards/{cardId}", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(adminCardService).deleteSystemCard(100L);
        }
    }
}
