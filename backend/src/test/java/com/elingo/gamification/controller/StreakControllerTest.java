package com.elingo.gamification.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.gamification.dto.response.StreakResponse;
import com.elingo.gamification.service.StreakService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class StreakControllerTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

    private MockMvc mockMvc;

    @Mock
    private StreakService streakService;

    @InjectMocks
    private StreakController streakController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(streakController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(CurrentUserId.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                  ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest,
                                                  WebDataBinderFactory binderFactory) {
                        return CURRENT_USER_ID;
                    }
                })
                .build();
    }

    @Nested
    @DisplayName("getUserStreak")
    class GetUserStreakTests {

        @Test
        @DisplayName("Get streak successfully when userId is not provided, defaults to current authenticated user")
        void getUserStreak_Success_WithoutUserId_UsesCurrentUserId() throws Exception {
            LocalDate today = LocalDate.now();
            StreakResponse mockResponse = new StreakResponse(
                    CURRENT_USER_ID,
                    5,
                    14,
                    today,
                    true,
                    3,
                    List.of(today.minusDays(2), today.minusDays(1), today)
            );

            when(streakService.getUserStreak(eq(CURRENT_USER_ID), isNull(), isNull())).thenReturn(mockResponse);

            mockMvc.perform(get("/gamification/streaks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(CURRENT_USER_ID))
                    .andExpect(jsonPath("$.data.currentStreak").value(5))
                    .andExpect(jsonPath("$.data.longestStreak").value(14))
                    .andExpect(jsonPath("$.data.lastActiveDate").value(today.toString()))
                    .andExpect(jsonPath("$.data.checkedInToday").value(true))
                    .andExpect(jsonPath("$.data.monthCheckinCount").value(3))
                    .andExpect(jsonPath("$.data.checkinDates.length()").value(3));

            verify(streakService).getUserStreak(CURRENT_USER_ID, null, null);
        }

        @Test
        @DisplayName("Get streak successfully when target userId is explicitly provided")
        void getUserStreak_Success_WithExplicitUserId_UsesGivenUserId() throws Exception {
            StreakResponse mockResponse = new StreakResponse(
                    TARGET_USER_ID,
                    10,
                    20,
                    LocalDate.of(2026, 10, 15),
                    false,
                    1,
                    List.of(LocalDate.of(2026, 10, 15))
            );

            when(streakService.getUserStreak(eq(TARGET_USER_ID), isNull(), isNull())).thenReturn(mockResponse);

            mockMvc.perform(get("/gamification/streaks")
                            .param("userId", String.valueOf(TARGET_USER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(TARGET_USER_ID))
                    .andExpect(jsonPath("$.data.currentStreak").value(10))
                    .andExpect(jsonPath("$.data.longestStreak").value(20))
                    .andExpect(jsonPath("$.data.checkedInToday").value(false))
                    .andExpect(jsonPath("$.data.monthCheckinCount").value(1));

            verify(streakService).getUserStreak(TARGET_USER_ID, null, null);
        }

        @Test
        @DisplayName("Get streak successfully with specified year and month parameters")
        void getUserStreak_Success_WithYearAndMonthParams() throws Exception {
            StreakResponse mockResponse = new StreakResponse(
                    CURRENT_USER_ID,
                    7,
                    10,
                    LocalDate.of(2026, 10, 20),
                    false,
                    2,
                    List.of(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 20))
            );

            when(streakService.getUserStreak(eq(CURRENT_USER_ID), eq(2026), eq(10))).thenReturn(mockResponse);

            mockMvc.perform(get("/gamification/streaks")
                            .param("year", "2026")
                            .param("month", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(CURRENT_USER_ID))
                    .andExpect(jsonPath("$.data.monthCheckinCount").value(2))
                    .andExpect(jsonPath("$.data.checkinDates[0]").value("2026-10-19"))
                    .andExpect(jsonPath("$.data.checkinDates[1]").value("2026-10-20"));

            verify(streakService).getUserStreak(CURRENT_USER_ID, 2026, 10);
        }

        @Test
        @DisplayName("Get streak successfully when user has zero checkins in the month")
        void getUserStreak_Success_WhenCheckinDatesIsEmpty() throws Exception {
            StreakResponse mockResponse = new StreakResponse(
                    CURRENT_USER_ID,
                    0,
                    0,
                    null,
                    false,
                    0,
                    List.of()
            );

            when(streakService.getUserStreak(eq(CURRENT_USER_ID), isNull(), isNull())).thenReturn(mockResponse);

            mockMvc.perform(get("/gamification/streaks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currentStreak").value(0))
                    .andExpect(jsonPath("$.data.longestStreak").value(0))
                    .andExpect(jsonPath("$.data.lastActiveDate").doesNotExist())
                    .andExpect(jsonPath("$.data.checkedInToday").value(false))
                    .andExpect(jsonPath("$.data.monthCheckinCount").value(0))
                    .andExpect(jsonPath("$.data.checkinDates").isArray())
                    .andExpect(jsonPath("$.data.checkinDates.length()").value(0));
        }

        @Test
        @DisplayName("Returns 404 NOT_FOUND when service throws AppException(USER_NOT_FOUND)")
        void getUserStreak_Fail_WhenUserNotFound_Returns404() throws Exception {
            Long nonExistentUserId = 999L;
            when(streakService.getUserStreak(eq(nonExistentUserId), any(), any()))
                    .thenThrow(new AppException(AppError.USER_NOT_FOUND));

            mockMvc.perform(get("/gamification/streaks")
                            .param("userId", String.valueOf(nonExistentUserId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("USER_NOT_FOUND"))
                    .andExpect(jsonPath("$.errors[0].message").value(AppError.USER_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("Returns 500 INTERNAL_SERVER_ERROR when service throws unexpected RuntimeException")
        void getUserStreak_Fail_WhenUnexpectedExceptionOccurs_Returns500() throws Exception {
            when(streakService.getUserStreak(any(), any(), any()))
                    .thenThrow(new RuntimeException("Database connection failure"));

            mockMvc.perform(get("/gamification/streaks"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("UNCATEGORIZED_EXCEPTION"));
        }
    }
}
