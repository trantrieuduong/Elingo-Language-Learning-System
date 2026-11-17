package com.elingo.user.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.CursorResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.common.service.RateLimiterService;
import com.elingo.community.dto.response.AuthorResponse;
import com.elingo.community.dto.response.PostResponse;
import com.elingo.community.entity.PostStatus;
import com.elingo.community.service.PostService;
import com.elingo.gamification.dto.response.MyLeaderboardRankResponse;
import com.elingo.gamification.dto.response.StreakResponse;
import com.elingo.gamification.dto.response.UserBadgeEarnedResponse;
import com.elingo.gamification.dto.response.UserLevelProgressResponse;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.entity.LeaderboardPeriodType;
import com.elingo.gamification.service.BadgeService;
import com.elingo.gamification.service.LeaderboardService;
import com.elingo.gamification.service.LevelService;
import com.elingo.gamification.service.StreakService;
import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.SetPasswordRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.dto.request.UpdateProfileRequest;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.entity.Role;
import com.elingo.user.service.UserService;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.service.DeckService;
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
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private DeckService deckService;

    @Mock
    private PostService postService;

    @Mock
    private RateLimiterService limiter;

    @Mock
    private BadgeService badgeService;

    @Mock
    private LeaderboardService leaderboardService;

    @Mock
    private LevelService levelService;

    @Mock
    private StreakService streakService;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(CurrentUserId.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return USER_ID;
                    }
                })
                .build();
    }

    @Nested
    @DisplayName("getMyInfo")
    class GetMyInfoTests {
        @Test
        @DisplayName("Get my info successfully")
        void getMyInfo_Success() throws Exception {
            UserMeResponse responseMock = new UserMeResponse(USER_ID, "testuser", "test@gmail.com", "Test User", null,
                    Role.USER, true, false, null, null, true, false);
            when(userService.getMyInfo(USER_ID)).thenReturn(responseMock);

            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }
    }

    @Nested
    @DisplayName("getMyDecks")
    class GetMyDecksTests {

        private DeckResponse buildDeckResponse(Long id, String title) {
            return new DeckResponse(
                    id,
                    title,
                    "deck-slug",
                    "Deck description",
                    false,
                    null,
                    DeckStatus.DRAFT,
                    OwnerType.USER,
                    USER_ID,
                    0,
                    0,
                    0,
                    null,
                    Set.of(),
                    Set.of()
            );
        }

        @Test
        @DisplayName("Get my decks successfully")
        void getMyDecks_Success() throws Exception {
            DeckResponse deck = buildDeckResponse(10L, "My Vocabulary Deck");
            when(deckService.getUserDecks(USER_ID)).thenReturn(List.of(deck));

            mockMvc.perform(get("/users/me/decks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(10))
                    .andExpect(jsonPath("$.data[0].title").value("My Vocabulary Deck"));

            verify(deckService).getUserDecks(USER_ID);
        }

        @Test
        @DisplayName("Get my decks successfully when empty")
        void getMyDecks_Success_Empty() throws Exception {
            when(deckService.getUserDecks(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/users/me/decks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(deckService).getUserDecks(USER_ID);
        }
    }

    @Nested
    @DisplayName("getUserPosts")
    class GetUserPostsTests {

        private PostResponse buildPostResponse(Long id) {
            return new PostResponse(
                    id,
                    1L,
                    "Post Title",
                    "Post Content",
                    PostStatus.PUBLISHED,
                    0,
                    0,
                    false,
                    new AuthorResponse(USER_ID, "testuser", "Test User", null),
                    List.of(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
        }

        @Test
        @DisplayName("Get user posts successfully with default parameters")
        void getUserPosts_Success_DefaultParams() throws Exception {
            Long targetUserId = 2L;
            PostResponse post = buildPostResponse(10L);
            CursorResponse<PostResponse> responseMock = new CursorResponse<>(List.of(post), "next-cursor-123", true);

            when(postService.getUserPosts(eq(targetUserId), isNull(), eq(20), eq(USER_ID)))
                    .thenReturn(responseMock);

            mockMvc.perform(get("/users/{id}/posts", targetUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andExpect(jsonPath("$.data.items[0].id").value(10L))
                    .andExpect(jsonPath("$.data.items[0].title").value("Post Title"))
                    .andExpect(jsonPath("$.data.nextCursor").value("next-cursor-123"))
                    .andExpect(jsonPath("$.data.hasMore").value(true));

            verify(postService).getUserPosts(targetUserId, null, 20, USER_ID);
        }

        @Test
        @DisplayName("Get user posts successfully with explicit cursor and limit")
        void getUserPosts_Success_CustomParams() throws Exception {
            Long targetUserId = 2L;
            CursorResponse<PostResponse> responseMock = new CursorResponse<>(List.of(), null, false);

            when(postService.getUserPosts(eq(targetUserId), eq("cursor123"), eq(10), eq(USER_ID)))
                    .thenReturn(responseMock);

            mockMvc.perform(get("/users/{id}/posts", targetUserId)
                            .param("cursor", "cursor123")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items.length()").value(0))
                    .andExpect(jsonPath("$.data.hasMore").value(false));

            verify(postService).getUserPosts(targetUserId, "cursor123", 10, USER_ID);
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserByIdTests {
        @Test
        @DisplayName("Get user by id successfully")
        void getUserById_Success() throws Exception {
            UserMeResponse responseMock = new UserMeResponse(USER_ID, "testuser", "test@gmail.com", "Test User", null,
                    Role.USER, true, false, null, null, true, false);
            when(userService.getUserById(USER_ID, USER_ID)).thenReturn(responseMock);

            mockMvc.perform(get("/users/{id}", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {
        @Test
        @DisplayName("Change password successfully")
        void changePassword_Success() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123@", "NewPassword123@");

            mockMvc.perform(patch("/users/me/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class));
        }
    }

    @Nested
    @DisplayName("setPassword")
    class SetPasswordTests {
        @Test
        @DisplayName("Set password successfully")
        void setPassword_Success() throws Exception {
            SetPasswordRequest request = new SetPasswordRequest("NewPassword123@");

            mockMvc.perform(post("/users/me/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).setPassword(eq(USER_ID), any(SetPasswordRequest.class));
        }
    }

    @Nested
    @DisplayName("sendOTPUpdateEmail")
    class SendOTPUpdateEmailTests {
        @Test
        @DisplayName("Send OTP successfully")
        void sendOTPUpdateEmail_Success() throws Exception {
            SendOTPUpdateEmailRequest request = new SendOTPUpdateEmailRequest("new@gmail.com");

            mockMvc.perform(post("/users/me/email/otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).sendOTPUpdateEmail(eq(USER_ID), any(SendOTPUpdateEmailRequest.class));
        }
    }

    @Nested
    @DisplayName("updateEmail")
    class UpdateEmailTests {
        @Test
        @DisplayName("Update email successfully")
        void updateEmail_Success() throws Exception {
            UpdateEmailRequest request = new UpdateEmailRequest("new@gmail.com", "123456", "Password123@");

            mockMvc.perform(patch("/users/me/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).updateEmail(eq(USER_ID), any(UpdateEmailRequest.class));
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfileTests {
        @Test
        @DisplayName("Update profile successfully")
        void updateProfile_Success() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest(null, "New Name", "newuser");
            UserMeResponse responseMock = new UserMeResponse(USER_ID, "newuser", "test@gmail.com", "New Name", null,
                    Role.USER, true, false, null, null, true, false);
            when(userService.updateProfile(USER_ID, request)).thenReturn(responseMock);

            mockMvc.perform(put("/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("newuser"))
                    .andExpect(jsonPath("$.data.fullName").value("New Name"));

            verify(userService).updateProfile(eq(USER_ID), any(UpdateProfileRequest.class));
        }
    }

    // ==========================================
    // Gamification Endpoints Tests
    // ==========================================

    @Nested
    @DisplayName("getMyBadges")
    class GetMyBadgesTests {

        @Test
        @DisplayName("Should return earned badges of current user successfully")
        void getMyBadges_Success() throws Exception {
            UserBadgeEarnedResponse badge = new UserBadgeEarnedResponse(
                    1L, "STREAK_7_DAYS", "7-Day Streak Master", "Desc", null, BadgeConditionType.STREAK_DAYS, 7, LocalDateTime.now()
            );

            when(badgeService.getUserBadges(USER_ID)).thenReturn(List.of(badge));

            mockMvc.perform(get("/users/me/badges"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].code").value("STREAK_7_DAYS"));

            verify(badgeService).getUserBadges(USER_ID);
        }
    }

    @Nested
    @DisplayName("getUserBadges")
    class GetUserBadgesTests {

        @Test
        @DisplayName("Should return earned badges of target user successfully")
        void getUserBadges_Success() throws Exception {
            UserBadgeEarnedResponse badge = new UserBadgeEarnedResponse(
                    2L, "TOTAL_XP_100", "XP Novice", "Desc", null, BadgeConditionType.TOTAL_XP, 100, LocalDateTime.now()
            );

            when(badgeService.getUserBadges(TARGET_USER_ID)).thenReturn(List.of(badge));

            mockMvc.perform(get("/users/{userId}/badges", TARGET_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].code").value("TOTAL_XP_100"));

            verify(badgeService).getUserBadges(TARGET_USER_ID);
        }

        @Test
        @DisplayName("Should return 404 when target user does not exist")
        void getUserBadges_UserNotFound() throws Exception {
            when(badgeService.getUserBadges(TARGET_USER_ID)).thenThrow(new AppException(AppError.USER_NOT_FOUND));

            mockMvc.perform(get("/users/{userId}/badges", TARGET_USER_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("USER_NOT_FOUND"));

            verify(badgeService).getUserBadges(TARGET_USER_ID);
        }
    }

    @Nested
    @DisplayName("getMyRank")
    class GetMyRankTests {

        @Test
        @DisplayName("Should return current user rank successfully")
        void getMyRank_Success() throws Exception {
            MyLeaderboardRankResponse rank = new MyLeaderboardRankResponse(
                    3, 500, 20, LeaderboardPeriodType.WEEKLY, "2026-W46"
            );

            when(leaderboardService.getMyRank(USER_ID, LeaderboardPeriodType.WEEKLY)).thenReturn(rank);

            mockMvc.perform(get("/users/me/rank"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.rank").value(3))
                    .andExpect(jsonPath("$.data.totalXp").value(500))
                    .andExpect(jsonPath("$.data.diffToNextRank").value(20));

            verify(leaderboardService).getMyRank(USER_ID, LeaderboardPeriodType.WEEKLY);
        }
    }

    @Nested
    @DisplayName("getMyLevels")
    class GetMyLevelsTests {

        @Test
        @DisplayName("Should return level progress for current user successfully")
        void getMyLevels_Success() throws Exception {
            UserLevelProgressResponse progress = new UserLevelProgressResponse(
                    USER_ID, (short) 2, "Explorer", 150, 100, (short) 3, "Challenger", 300, 150, 25.0
            );

            when(levelService.getUserLevelProgress(USER_ID)).thenReturn(progress);

            mockMvc.perform(get("/users/me/levels"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(USER_ID))
                    .andExpect(jsonPath("$.data.currentLevel").value(2))
                    .andExpect(jsonPath("$.data.progressPercent").value(25.0));

            verify(levelService).getUserLevelProgress(USER_ID);
        }
    }

    @Nested
    @DisplayName("getUserLevels")
    class GetUserLevelsTests {

        @Test
        @DisplayName("Should return level progress for target user successfully")
        void getUserLevels_Success() throws Exception {
            UserLevelProgressResponse progress = new UserLevelProgressResponse(
                    TARGET_USER_ID, (short) 1, "Novice", 50, 0, (short) 2, "Explorer", 100, 50, 50.0
            );

            when(levelService.getUserLevelProgress(TARGET_USER_ID)).thenReturn(progress);

            mockMvc.perform(get("/users/{userId}/levels", TARGET_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(TARGET_USER_ID))
                    .andExpect(jsonPath("$.data.currentLevel").value(1));

            verify(levelService).getUserLevelProgress(TARGET_USER_ID);
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void getUserLevels_UserNotFound() throws Exception {
            when(levelService.getUserLevelProgress(TARGET_USER_ID)).thenThrow(new AppException(AppError.USER_NOT_FOUND));

            mockMvc.perform(get("/users/{userId}/levels", TARGET_USER_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("USER_NOT_FOUND"));

            verify(levelService).getUserLevelProgress(TARGET_USER_ID);
        }
    }

    @Nested
    @DisplayName("getMyStreaks")
    class GetMyStreaksTests {

        @Test
        @DisplayName("Should return current user streak progress successfully")
        void getMyStreaks_Success() throws Exception {
            StreakResponse response = new StreakResponse(
                    USER_ID, 5, 10, LocalDate.now(), true, 5, List.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2))
            );

            when(streakService.getUserStreak(eq(USER_ID), isNull(), isNull())).thenReturn(response);

            mockMvc.perform(get("/users/me/streaks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currentStreak").value(5))
                    .andExpect(jsonPath("$.data.longestStreak").value(10))
                    .andExpect(jsonPath("$.data.checkedInToday").value(true));

            verify(streakService).getUserStreak(eq(USER_ID), isNull(), isNull());
        }
    }

    @Nested
    @DisplayName("getUserStreaks")
    class GetUserStreaksTests {

        @Test
        @DisplayName("Should return target user streak progress successfully")
        void getUserStreaks_Success() throws Exception {
            StreakResponse response = new StreakResponse(
                    TARGET_USER_ID, 2, 4, LocalDate.now(), false, 2, List.of(LocalDate.of(2026, 11, 1))
            );

            when(streakService.getUserStreak(eq(TARGET_USER_ID), eq(2026), eq(11))).thenReturn(response);

            mockMvc.perform(get("/users/{userId}/streaks", TARGET_USER_ID)
                            .param("year", "2026")
                            .param("month", "11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(TARGET_USER_ID))
                    .andExpect(jsonPath("$.data.currentStreak").value(2));

            verify(streakService).getUserStreak(eq(TARGET_USER_ID), eq(2026), eq(11));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void getUserStreaks_UserNotFound() throws Exception {
            when(streakService.getUserStreak(eq(TARGET_USER_ID), isNull(), isNull()))
                    .thenThrow(new AppException(AppError.USER_NOT_FOUND));

            mockMvc.perform(get("/users/{userId}/streaks", TARGET_USER_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("USER_NOT_FOUND"));

            verify(streakService).getUserStreak(eq(TARGET_USER_ID), isNull(), isNull());
        }
    }
}
