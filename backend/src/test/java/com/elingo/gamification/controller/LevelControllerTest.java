package com.elingo.gamification.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.gamification.dto.response.LevelResponse;
import com.elingo.gamification.dto.response.UserLevelProgressResponse;
import com.elingo.gamification.service.LevelService;
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
@DisplayName("LevelController Tests")
class LevelControllerTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long TARGET_USER_ID = 10L;

    private MockMvc mockMvc;

    @Mock
    private LevelService levelService;

    @InjectMocks
    private LevelController levelController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(levelController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(CurrentUserId.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return CURRENT_USER_ID;
                    }
                })
                .build();
    }

    @Nested
    @DisplayName("getAllLevels")
    class GetAllLevelsTests {

        @Test
        @DisplayName("Should return all configured levels roadmap successfully")
        void getAllLevels_Success() throws Exception {
            List<LevelResponse> mockLevels = List.of(
                    new LevelResponse((short) 1, 0, "Novice"),
                    new LevelResponse((short) 2, 100, "Explorer")
            );

            when(levelService.getAllLevels()).thenReturn(mockLevels);

            mockMvc.perform(get("/gamification/levels"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].levelNo").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("Novice"))
                    .andExpect(jsonPath("$.data[1].levelNo").value(2))
                    .andExpect(jsonPath("$.data[1].requiredXp").value(100));

            verify(levelService).getAllLevels();
        }
    }

    @Nested
    @DisplayName("getUserLevelProgress")
    class GetUserLevelProgressTests {

        @Test
        @DisplayName("Should retrieve level progress for explicit target userId successfully")
        void getUserLevelProgress_Success_WithExplicitUserId() throws Exception {
            UserLevelProgressResponse mockProgress = new UserLevelProgressResponse(
                    TARGET_USER_ID,
                    (short) 1,
                    "Novice",
                    50,
                    0,
                    (short) 2,
                    "Explorer",
                    100,
                    50,
                    50.0
            );

            when(levelService.getUserLevelProgress(TARGET_USER_ID)).thenReturn(mockProgress);

            mockMvc.perform(get("/gamification/levels/progress")
                            .param("userId", String.valueOf(TARGET_USER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(TARGET_USER_ID))
                    .andExpect(jsonPath("$.data.currentLevel").value(1))
                    .andExpect(jsonPath("$.data.progressPercent").value(50.0));

            verify(levelService).getUserLevelProgress(TARGET_USER_ID);
        }

        @Test
        @DisplayName("Should fallback to authenticated user ID when userId parameter is omitted")
        void getUserLevelProgress_Success_WithoutUserId_UsesCurrentUserId() throws Exception {
            UserLevelProgressResponse mockProgress = new UserLevelProgressResponse(
                    CURRENT_USER_ID,
                    (short) 3,
                    "Challenger",
                    350,
                    300,
                    (short) 4,
                    "Veteran",
                    600,
                    250,
                    16.7
            );

            when(levelService.getUserLevelProgress(CURRENT_USER_ID)).thenReturn(mockProgress);

            mockMvc.perform(get("/gamification/levels/progress"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(CURRENT_USER_ID))
                    .andExpect(jsonPath("$.data.currentLevel").value(3))
                    .andExpect(jsonPath("$.data.currentTitle").value("Challenger"))
                    .andExpect(jsonPath("$.data.totalXp").value(350))
                    .andExpect(jsonPath("$.data.nextLevel").value(4))
                    .andExpect(jsonPath("$.data.progressPercent").value(16.7));

            verify(levelService).getUserLevelProgress(CURRENT_USER_ID);
        }

        @Test
        @DisplayName("Should return 404 when target user does not exist")
        void getUserLevelProgress_UserNotFound_WithExplicitUserId() throws Exception {
            when(levelService.getUserLevelProgress(TARGET_USER_ID))
                    .thenThrow(new AppException(AppError.USER_NOT_FOUND));

            mockMvc.perform(get("/gamification/levels/progress")
                            .param("userId", String.valueOf(TARGET_USER_ID)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("USER_NOT_FOUND"));

            verify(levelService).getUserLevelProgress(TARGET_USER_ID);
        }

        @Test
        @DisplayName("Should return 404 when current authenticated user is not found")
        void getUserLevelProgress_UserNotFound_WithoutUserId() throws Exception {
            when(levelService.getUserLevelProgress(CURRENT_USER_ID))
                    .thenThrow(new AppException(AppError.USER_NOT_FOUND));

            mockMvc.perform(get("/gamification/levels/progress"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("USER_NOT_FOUND"));

            verify(levelService).getUserLevelProgress(CURRENT_USER_ID);
        }
    }
}
