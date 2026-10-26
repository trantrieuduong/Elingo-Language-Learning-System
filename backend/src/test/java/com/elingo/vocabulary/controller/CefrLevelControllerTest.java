package com.elingo.vocabulary.controller;

import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.vocabulary.dto.response.CefrLevelResponse;
import com.elingo.vocabulary.service.CefrLevelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CefrLevelControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CefrLevelService cefrLevelService;

    @InjectMocks
    private CefrLevelController cefrLevelController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cefrLevelController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("getAllCefrLevels")
    class GetAllCefrLevelsTests {

        @Test
        @DisplayName("Get all CEFR levels successfully")
        void getAllCefrLevels_Success() throws Exception {
            when(cefrLevelService.getAllCefrLevels()).thenReturn(List.of(
                    new CefrLevelResponse(1L, "Beginner"),
                    new CefrLevelResponse(2L, "Upper-Intermediate")));

            mockMvc.perform(get("/cefr-levels"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].label").value("Beginner"))
                    .andExpect(jsonPath("$.data[1].id").value(2))
                    .andExpect(jsonPath("$.data[1].label").value("Upper-Intermediate"));

            verify(cefrLevelService).getAllCefrLevels();
        }

        @Test
        @DisplayName("Get all CEFR levels successfully: returns empty list")
        void getAllCefrLevels_Success_EmptyList() throws Exception {
            when(cefrLevelService.getAllCefrLevels()).thenReturn(List.of());

            mockMvc.perform(get("/cefr-levels"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(cefrLevelService).getAllCefrLevels();
        }
    }
}
