package com.elingo.admin.content.controller;

import com.elingo.vocabulary.dto.request.CreateSystemTopicRequest;
import com.elingo.vocabulary.dto.request.ReorderItemRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemTopicRequest;
import com.elingo.admin.content.service.AdminCardService;
import com.elingo.admin.content.service.AdminTopicService;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminTopicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminTopicService adminTopicService;

    @Mock
    private AdminCardService adminCardService;

    @InjectMocks
    private AdminTopicController adminTopicController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminTopicController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private TopicResponse sampleTopicResponse(Long id, String name) {
        return new TopicResponse(id, name, "slug-" + id, 1, 0, 0);
    }

    @Nested
    @DisplayName("createSystemTopic")
    class CreateSystemTopicTests {

        @Test
        @DisplayName("Create system topic successfully")
        void createSystemTopic_Success() throws Exception {
            CreateSystemTopicRequest request = new CreateSystemTopicRequest(10L, "New Topic");
            when(adminTopicService.createSystemTopic(any(CreateSystemTopicRequest.class)))
                    .thenReturn(sampleTopicResponse(1L, "New Topic"));

            mockMvc.perform(post("/admin/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.name").value("New Topic"));

            verify(adminTopicService).createSystemTopic(any(CreateSystemTopicRequest.class));
        }

        @Test
        @DisplayName("Create system topic fails validation when deckId is missing")
        void createSystemTopic_Validation_MissingDeckId() throws Exception {
            CreateSystemTopicRequest request = new CreateSystemTopicRequest(null, "Topic Name");

            mockMvc.perform(post("/admin/topics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(adminTopicService, never()).createSystemTopic(any());
        }
    }

    @Nested
    @DisplayName("updateSystemTopic")
    class UpdateSystemTopicTests {

        @Test
        @DisplayName("Update system topic successfully")
        void updateSystemTopic_Success() throws Exception {
            UpdateSystemTopicRequest request = new UpdateSystemTopicRequest("Renamed Topic");
            when(adminTopicService.updateSystemTopic(eq(5L), any(UpdateSystemTopicRequest.class)))
                    .thenReturn(sampleTopicResponse(5L, "Renamed Topic"));

            mockMvc.perform(put("/admin/topics/{topicId}", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Renamed Topic"));

            verify(adminTopicService).updateSystemTopic(eq(5L), any(UpdateSystemTopicRequest.class));
        }

        @Test
        @DisplayName("Update system topic fails when topic not found")
        void updateSystemTopic_NotFound() throws Exception {
            UpdateSystemTopicRequest request = new UpdateSystemTopicRequest("Renamed Topic");
            when(adminTopicService.updateSystemTopic(eq(99L), any(UpdateSystemTopicRequest.class)))
                    .thenThrow(new AppException(AppError.TOPIC_NOT_FOUND));

            mockMvc.perform(put("/admin/topics/{topicId}", 99L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TOPIC_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("deleteSystemTopic")
    class DeleteSystemTopicTests {

        @Test
        @DisplayName("Delete system topic successfully")
        void deleteSystemTopic_Success() throws Exception {
            mockMvc.perform(delete("/admin/topics/{topicId}", 5L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(adminTopicService).deleteSystemTopic(5L);
        }
    }

    @Nested
    @DisplayName("getCardsByTopicId")
    class GetCardsByTopicIdTests {

        @Test
        @DisplayName("Get cards by topic ID successfully")
        void getCardsByTopicId_Success() throws Exception {
            List<CardResponse> cards = List.of(
                    new CardResponse(1L, 1, "term", "noun", "nghĩa", null, null, null, null, null, List.of(), null)
            );
            when(adminCardService.getCardsByTopicId(10L)).thenReturn(cards);

            mockMvc.perform(get("/admin/topics/{topicId}/cards", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(adminCardService).getCardsByTopicId(10L);
        }
    }

    @Nested
    @DisplayName("reorderSystemCards")
    class ReorderSystemCardsTests {

        @Test
        @DisplayName("Reorder system cards successfully")
        void reorderSystemCards_Success() throws Exception {
            ReorderItemRequest request = new ReorderItemRequest(List.of(2L, 1L));

            mockMvc.perform(patch("/admin/topics/{topicId}/cards/orders", 10L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(adminCardService).reorderSystemCards(eq(10L), any(ReorderItemRequest.class));
        }
    }
}
