package com.elingo.gamification.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.gamification.dto.response.UserBadgeEarnedResponse;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.service.BadgeService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BadgeControllerTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

    private MockMvc mockMvc;

    @Mock
    private BadgeService badgeService;

    @InjectMocks
    private BadgeController badgeController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(badgeController)
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
    @DisplayName("getUserBadges - Success cases")
    class GetUserBadgesSuccessTests {

        @Test
        @DisplayName("Get earned badges successfully without userId parameter, defaults to current user")
        void getUserBadges_Success_WithoutUserId_UsesCurrentUserId() throws Exception {
            LocalDateTime earnedTime = LocalDateTime.of(2026, 11, 16, 10, 30);
            List<UserBadgeEarnedResponse> mockBadges = List.of(
                    new UserBadgeEarnedResponse(
                            1L,
                            "STREAK_7_DAYS",
                            "7-Day Streak Master",
                            "Maintain a learning streak for 7 consecutive days",
                            null,
                            BadgeConditionType.STREAK_DAYS,
                            7,
                            earnedTime
                    )
            );

            when(badgeService.getUserBadges(CURRENT_USER_ID)).thenReturn(mockBadges);

            mockMvc.perform(get("/gamification/badges"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].code").value("STREAK_7_DAYS"))
                    .andExpect(jsonPath("$.data[0].name").value("7-Day Streak Master"))
                    .andExpect(jsonPath("$.data[0].conditionType").value("STREAK_DAYS"))
                    .andExpect(jsonPath("$.data[0].conditionValue").value(7))
                    .andExpect(jsonPath("$.data[0].earnedAt").value(startsWith("2026-11-16T10:30")));

            verify(badgeService).getUserBadges(CURRENT_USER_ID);
        }

        @Test
        @DisplayName("Get earned badges successfully with explicit userId parameter")
        void getUserBadges_Success_WithExplicitUserId_UsesGivenUserId() throws Exception {
            LocalDateTime earnedTime = LocalDateTime.of(2026, 11, 15, 18, 0);
            List<UserBadgeEarnedResponse> mockBadges = List.of(
                    new UserBadgeEarnedResponse(
                            2L,
                            "TOTAL_XP_1000",
                            "XP Novice",
                            "Accumulate 1,000 total experience points",
                            null,
                            BadgeConditionType.TOTAL_XP,
                            1000,
                            earnedTime
                    )
            );

            when(badgeService.getUserBadges(TARGET_USER_ID)).thenReturn(mockBadges);

            mockMvc.perform(get("/gamification/badges")
                            .param("userId", String.valueOf(TARGET_USER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(2))
                    .andExpect(jsonPath("$.data[0].code").value("TOTAL_XP_1000"));

            verify(badgeService).getUserBadges(TARGET_USER_ID);
        }

        @Test
        @DisplayName("Get earned badges successfully when user has not earned any badges yet")
        void getUserBadges_Success_WhenUserHasNoBadges_ReturnsEmptyArray() throws Exception {
            when(badgeService.getUserBadges(CURRENT_USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/gamification/badges"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(badgeService).getUserBadges(CURRENT_USER_ID);
        }
    }

    @Nested
    @DisplayName("getUserBadges - Failure cases")
    class GetUserBadgesFailureTests {

        @Test
        @DisplayName("Returns 404 NOT_FOUND when service throws AppException(USER_NOT_FOUND)")
        void getUserBadges_Fail_WhenUserNotFound_Returns404() throws Exception {
            Long nonExistentUserId = 999L;
            when(badgeService.getUserBadges(eq(nonExistentUserId)))
                    .thenThrow(new AppException(AppError.USER_NOT_FOUND));

            mockMvc.perform(get("/gamification/badges")
                            .param("userId", String.valueOf(nonExistentUserId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("USER_NOT_FOUND"))
                    .andExpect(jsonPath("$.errors[0].message").value(AppError.USER_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("Returns 500 INTERNAL_SERVER_ERROR when service throws unexpected RuntimeException")
        void getUserBadges_Fail_WhenUnexpectedExceptionOccurs_Returns500() throws Exception {
            when(badgeService.getUserBadges(any()))
                    .thenThrow(new RuntimeException("Database connection failure"));

            mockMvc.perform(get("/gamification/badges"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("UNCATEGORIZED_EXCEPTION"));
        }
    }
}
