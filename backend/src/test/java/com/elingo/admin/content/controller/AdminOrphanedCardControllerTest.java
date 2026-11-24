package com.elingo.admin.content.controller;

import com.elingo.vocabulary.dto.response.OrphanedCardCleanupResponse;
import com.elingo.vocabulary.dto.response.OrphanedCardStatsResponse;
import com.elingo.admin.content.service.AdminOrphanedCardService;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.vocabulary.dto.response.CardResponse;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminOrphanedCardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminOrphanedCardService adminOrphanedCardService;

    @InjectMocks
    private AdminOrphanedCardController adminOrphanedCardController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminOrphanedCardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("getStats")
    class GetStatsTests {

        @Test
        @DisplayName("Get orphaned card stats successfully")
        void getStats_Success() throws Exception {
            OrphanedCardStatsResponse response = new OrphanedCardStatsResponse(15L, 10L, 5L);
            when(adminOrphanedCardService.getStats()).thenReturn(response);

            mockMvc.perform(get("/admin/orphaned-cards/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalOrphanedCards").value(15L))
                    .andExpect(jsonPath("$.data.safeToDeleteCards").value(10L))
                    .andExpect(jsonPath("$.data.retainedInStudyCards").value(5L));

            verify(adminOrphanedCardService).getStats();
        }
    }

    @Nested
    @DisplayName("getOrphanedCards")
    class GetOrphanedCardsTests {

        @Test
        @DisplayName("Get orphaned cards page successfully")
        void getOrphanedCards_Success() throws Exception {
            CardResponse card = new CardResponse(
                    1L, 1, "orphan", "noun", "nghĩa", null, null, null, null, null, List.of(), null
            );
            PageResponse<CardResponse> pageResponse = new PageResponse<>(
                    List.of(card), 1, 20, 1, 1, true
            );
            when(adminOrphanedCardService.getOrphanedCards(any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/admin/orphaned-cards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].term").value("orphan"));

            verify(adminOrphanedCardService).getOrphanedCards(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("cleanupOrphanedCards")
    class CleanupOrphanedCardsTests {

        @Test
        @DisplayName("Cleanup orphaned cards successfully")
        void cleanupOrphanedCards_Success() throws Exception {
            OrphanedCardCleanupResponse response = new OrphanedCardCleanupResponse(10, 8, 3);
            when(adminOrphanedCardService.cleanupOrphanedCards()).thenReturn(response);

            mockMvc.perform(post("/admin/orphaned-cards/cleanup"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.scannedCount").value(10))
                    .andExpect(jsonPath("$.data.deletedCardCount").value(8))
                    .andExpect(jsonPath("$.data.deletedFileCount").value(3));

            verify(adminOrphanedCardService).cleanupOrphanedCards();
        }
    }
}
