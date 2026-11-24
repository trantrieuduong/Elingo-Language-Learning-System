package com.elingo.admin.content.controller;

import com.elingo.vocabulary.dto.request.AdminDeckSearchRequest;
import com.elingo.vocabulary.dto.request.ReorderItemRequest;
import com.elingo.vocabulary.dto.request.SystemDeckRequest;
import com.elingo.vocabulary.dto.request.UpdateDeckStatusRequest;
import com.elingo.admin.content.service.AdminDeckService;
import com.elingo.admin.content.service.AdminTopicService;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminDeckControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminDeckService adminDeckService;

    @Mock
    private AdminTopicService adminTopicService;

    @InjectMocks
    private AdminDeckController adminDeckController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminDeckController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private DeckResponse sampleDeckResponse(Long id, String title) {
        return new DeckResponse(
                id,
                title,
                "slug-" + id,
                "Description",
                false,
                null,
                DeckStatus.PUBLISHED,
                OwnerType.SYSTEM,
                null,
                0,
                0,
                0,
                LocalDateTime.now(),
                Set.of(),
                Set.of()
        );
    }

    @Nested
    @DisplayName("searchDecks")
    class SearchDecksTests {

        @Test
        @DisplayName("Search decks successfully with pagination")
        void searchDecks_Success() throws Exception {
            PageResponse<DeckResponse> pageResponse = new PageResponse<>(
                    List.of(sampleDeckResponse(1L, "Deck 1")),
                    1, 20, 1, 1, true
            );
            when(adminDeckService.searchDecks(any(AdminDeckSearchRequest.class), any(Pageable.class)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/admin/decks")
                            .param("keyword", "Deck")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].id").value(1L));

            verify(adminDeckService).searchDecks(any(AdminDeckSearchRequest.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getDeckById")
    class GetDeckByIdTests {

        @Test
        @DisplayName("Get deck by ID successfully")
        void getDeckById_Success() throws Exception {
            when(adminDeckService.getDeckById(10L)).thenReturn(sampleDeckResponse(10L, "Found Deck"));

            mockMvc.perform(get("/admin/decks/{deckId}", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(10L))
                    .andExpect(jsonPath("$.data.title").value("Found Deck"));

            verify(adminDeckService).getDeckById(10L);
        }

        @Test
        @DisplayName("Get deck by ID fails when deck not found")
        void getDeckById_NotFound() throws Exception {
            when(adminDeckService.getDeckById(99L)).thenThrow(new AppException(AppError.DECK_NOT_FOUND));

            mockMvc.perform(get("/admin/decks/{deckId}", 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("getTopicsByDeckId")
    class GetTopicsByDeckIdTests {

        @Test
        @DisplayName("Get topics by deck ID successfully")
        void getTopicsByDeckId_Success() throws Exception {
            List<TopicResponse> topics = List.of(
                    new TopicResponse(1L, "Topic 1", "slug-1", 1, 0, 0)
            );
            when(adminTopicService.getTopicsByDeckId(10L)).thenReturn(topics);

            mockMvc.perform(get("/admin/decks/{deckId}/topics", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(adminTopicService).getTopicsByDeckId(10L);
        }
    }

    @Nested
    @DisplayName("reorderSystemTopics")
    class ReorderSystemTopicsTests {

        @Test
        @DisplayName("Reorder system topics successfully")
        void reorderSystemTopics_Success() throws Exception {
            ReorderItemRequest request = new ReorderItemRequest(List.of(2L, 1L));

            mockMvc.perform(patch("/admin/decks/{deckId}/topics/orders", 10L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(adminTopicService).reorderSystemTopics(eq(10L), any(ReorderItemRequest.class));
        }

        @Test
        @DisplayName("Reorder system topics fails when itemIds is empty")
        void reorderSystemTopics_Validation_EmptyList() throws Exception {
            ReorderItemRequest request = new ReorderItemRequest(List.of());

            mockMvc.perform(patch("/admin/decks/{deckId}/topics/orders", 10L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(adminTopicService, never()).reorderSystemTopics(any(), any());
        }
    }

    @Nested
    @DisplayName("createSystemDeck")
    class CreateSystemDeckTests {

        @Test
        @DisplayName("Create system deck successfully")
        void createSystemDeck_Success() throws Exception {
            SystemDeckRequest request = new SystemDeckRequest("New Deck", "Desc", null, false, Set.of(), Set.of());
            when(adminDeckService.createSystemDeck(any(SystemDeckRequest.class)))
                    .thenReturn(sampleDeckResponse(100L, "New Deck"));

            mockMvc.perform(post("/admin/decks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(100L))
                    .andExpect(jsonPath("$.data.title").value("New Deck"));

            verify(adminDeckService).createSystemDeck(any(SystemDeckRequest.class));
        }

        @Test
        @DisplayName("Create system deck fails validation when title is blank")
        void createSystemDeck_Validation_BlankTitle() throws Exception {
            SystemDeckRequest request = new SystemDeckRequest("", "Desc", null, false, Set.of(), Set.of());

            mockMvc.perform(post("/admin/decks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(adminDeckService, never()).createSystemDeck(any());
        }
    }

    @Nested
    @DisplayName("updateSystemDeck")
    class UpdateSystemDeckTests {

        @Test
        @DisplayName("Update system deck successfully")
        void updateSystemDeck_Success() throws Exception {
            SystemDeckRequest request = new SystemDeckRequest("Updated Deck", "Desc", null, false, Set.of(), Set.of());
            when(adminDeckService.updateSystemDeck(eq(10L), any(SystemDeckRequest.class)))
                    .thenReturn(sampleDeckResponse(10L, "Updated Deck"));

            mockMvc.perform(put("/admin/decks/{deckId}", 10L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Updated Deck"));

            verify(adminDeckService).updateSystemDeck(eq(10L), any(SystemDeckRequest.class));
        }
    }

    @Nested
    @DisplayName("updateDeckStatus")
    class UpdateDeckStatusTests {

        @Test
        @DisplayName("Update deck status successfully")
        void updateDeckStatus_Success() throws Exception {
            UpdateDeckStatusRequest request = new UpdateDeckStatusRequest(DeckStatus.PUBLISHED);
            when(adminDeckService.updateStatus(eq(10L), any(UpdateDeckStatusRequest.class)))
                    .thenReturn(sampleDeckResponse(10L, "Deck"));

            mockMvc.perform(patch("/admin/decks/{deckId}/status", 10L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(adminDeckService).updateStatus(eq(10L), any(UpdateDeckStatusRequest.class));
        }

        @Test
        @DisplayName("Update deck status fails validation when status is null")
        void updateDeckStatus_Validation_NullStatus() throws Exception {
            mockMvc.perform(patch("/admin/decks/{deckId}/status", 10L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(adminDeckService, never()).updateStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("deleteUserDeck")
    class DeleteUserDeckTests {

        @Test
        @DisplayName("Delete user deck by admin successfully")
        void deleteUserDeck_Success() throws Exception {
            mockMvc.perform(delete("/admin/decks/{deckId}", 10L)
                            .param("reason", "Violation"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(adminDeckService).deleteUserDeck(10L, "Violation");
        }

        @Test
        @DisplayName("Delete user deck with default reason when not specified")
        void deleteUserDeck_DefaultReason() throws Exception {
            mockMvc.perform(delete("/admin/decks/{deckId}", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(adminDeckService).deleteUserDeck(10L, "Policy violation");
        }
    }
}
