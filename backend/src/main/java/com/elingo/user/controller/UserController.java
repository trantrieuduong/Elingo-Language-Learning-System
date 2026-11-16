package com.elingo.user.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.CursorResponse;
import com.elingo.common.enums.RateLimitRule;
import com.elingo.common.service.RateLimiterService;
import com.elingo.community.dto.response.PostResponse;
import com.elingo.community.service.PostService;
import com.elingo.gamification.dto.response.MyLeaderboardRankResponse;
import com.elingo.gamification.dto.response.StreakResponse;
import com.elingo.gamification.dto.response.UserBadgeEarnedResponse;
import com.elingo.gamification.dto.response.UserLevelProgressResponse;
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
import com.elingo.user.service.UserService;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.service.DeckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(
        name = "User Controller",
        description = "Endpoints for user profile, authentication settings, social posts, vocabulary decks, and gamification progress"
)
public class UserController {

    private final UserService userService;
    private final DeckService deckService;
    private final PostService postService;
    private final RateLimiterService limiter;
    private final BadgeService badgeService;
    private final LeaderboardService leaderboardService;
    private final LevelService levelService;
    private final StreakService streakService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ApiResponse<UserMeResponse> getMyInfo(@CurrentUserId Long userId) {
        UserMeResponse response = userService.getMyInfo(userId);
        return ApiResponse.<UserMeResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/me/decks")
    @Operation(summary = "Get current user decks")
    public ApiResponse<List<DeckResponse>> getMyDecks(@CurrentUserId Long userId) {
        return ApiResponse.<List<DeckResponse>>builder()
                .success(true)
                .data(deckService.getUserDecks(userId))
                .build();
    }

    @GetMapping("/{id}/posts")
    @Operation(summary = "Get users posts by id")
    public ApiResponse<CursorResponse<PostResponse>> getUserPosts(
            @PathVariable Long id,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<CursorResponse<PostResponse>>builder()
                .success(true)
                .data(postService.getUserPosts(id, cursor, limit, currentUserId))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile by id")
    public ApiResponse<Object> getUserById(
            @PathVariable Long id,
            @CurrentUserId Long currentUserId) {
        Object response = userService.getUserById(id, currentUserId);
        return ApiResponse.builder()
                .success(true)
                .data(response)
                .build();
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Change current user password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @CurrentUserId Long userId) {
        userService.changePassword(userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping("/me/password")
    @Operation(summary = "Set password for Google-authenticated account (first time only)")
    public ApiResponse<Void> setPassword(
            @Valid @RequestBody SetPasswordRequest request,
            @CurrentUserId Long userId) {
        userService.setPassword(userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping("/me/email/otp")
    @Operation(summary = "Send OTP for update email via email")
    public ApiResponse<Void> sendOTPUpdateEmail(
            @Valid @RequestBody SendOTPUpdateEmailRequest request,
            @CurrentUserId Long userId) {
        limiter.check(RateLimitRule.OTP_SEND_EMAIL, request.newEmail());

        userService.sendOTPUpdateEmail(userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PatchMapping("/me/email")
    @Operation(summary = "Confirm email update with OTP")
    public ApiResponse<Void> updateEmail(
            @Valid @RequestBody UpdateEmailRequest request,
            @CurrentUserId Long userId) {
        limiter.check(RateLimitRule.OTP_RESET_EMAIL, request.newEmail());

        userService.updateEmail(userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ApiResponse<UserMeResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @CurrentUserId Long userId) {

        UserMeResponse response = userService.updateProfile(userId, request);
        return ApiResponse.<UserMeResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    // ==========================================
    // Gamification Endpoints
    // ==========================================

    @GetMapping("/me/badges")
    @Operation(
            summary = "Get earned badges of current user",
            description = "Retrieves the list of badges earned by the current authenticated user, ordered by most recent earned date."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Earned badges retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "MyBadgeListSuccessResponse",
                            summary = "Sample list of earned badges for current user",
                            value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "id": 1,
                                          "code": "STREAK_7_DAYS",
                                          "name": "7-Day Streak Master",
                                          "description": "Maintain a learning streak for 7 consecutive days",
                                          "iconUrl": "https://cdn.elingo.com/badges/streak-7.png",
                                          "conditionType": "STREAK_DAYS",
                                          "conditionValue": 7,
                                          "earnedAt": "2026-11-16T10:30:00"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<List<UserBadgeEarnedResponse>> getMyBadges(@CurrentUserId Long userId) {
        List<UserBadgeEarnedResponse> response = badgeService.getUserBadges(userId);
        return ApiResponse.<List<UserBadgeEarnedResponse>>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/{userId}/badges")
    @Operation(
            summary = "Get earned badges of a user",
            description = "Retrieves the list of badges earned by the specified user ID, ordered by most recent earned date."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Earned badges retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UserBadgeListSuccessResponse",
                            summary = "Sample list of earned badges for specified user",
                            value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "id": 1,
                                          "code": "STREAK_7_DAYS",
                                          "name": "7-Day Streak Master",
                                          "description": "Maintain a learning streak for 7 consecutive days",
                                          "iconUrl": "https://cdn.elingo.com/badges/streak-7.png",
                                          "conditionType": "STREAK_DAYS",
                                          "conditionValue": 7,
                                          "earnedAt": "2026-11-16T10:30:00"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "UserNotFoundResponse",
                            summary = "User not found response",
                            value = """
                                    {
                                      "success": false,
                                      "errors": [
                                        {
                                          "code": "USER_NOT_FOUND",
                                          "message": "User not found"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<List<UserBadgeEarnedResponse>> getUserBadges(
            @Parameter(description = "User ID to retrieve badges for", example = "1", required = true)
            @PathVariable Long userId) {
        List<UserBadgeEarnedResponse> response = badgeService.getUserBadges(userId);
        return ApiResponse.<List<UserBadgeEarnedResponse>>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/me/rank")
    @Operation(
            summary = "Get current user's leaderboard rank",
            description = "Retrieves the authenticated user's ranking position, accumulated XP, and the XP gap to the next higher rank in the specified period."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User rank retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "MyRankSuccessResponse",
                            summary = "Sample user rank response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "rank": 5,
                                        "totalXp": 850,
                                        "diffToNextRank": 50,
                                        "periodType": "WEEKLY",
                                        "periodKey": "2026-W46"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<MyLeaderboardRankResponse> getMyRank(
            @CurrentUserId Long userId,
            @Parameter(description = "Leaderboard time period (WEEKLY or MONTHLY). Defaults to WEEKLY.", example = "WEEKLY")
            @RequestParam(defaultValue = "WEEKLY") LeaderboardPeriodType periodType) {
        MyLeaderboardRankResponse response = leaderboardService.getMyRank(userId, periodType);
        return ApiResponse.<MyLeaderboardRankResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/me/levels")
    @Operation(
            summary = "Get current user level progress",
            description = "Retrieves level progression details for the authenticated user, including current level, total XP, next level target, remaining XP needed, and completion percentage."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User level progress retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "MyLevelProgressSuccessResponse",
                            summary = "Sample user level progress response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "userId": 1,
                                        "currentLevel": 2,
                                        "currentTitle": "Explorer",
                                        "totalXp": 150,
                                        "currentLevelRequiredXp": 100,
                                        "nextLevel": 3,
                                        "nextLevelTitle": "Challenger",
                                        "nextLevelRequiredXp": 300,
                                        "xpToNextLevel": 150,
                                        "progressPercent": 25.0
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<UserLevelProgressResponse> getMyLevelProgress(@CurrentUserId Long userId) {
        UserLevelProgressResponse response = levelService.getUserLevelProgress(userId);
        return ApiResponse.<UserLevelProgressResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/{userId}/levels")
    @Operation(
            summary = "Get user level progress by ID",
            description = "Retrieves level progression details for the specified user ID, including current level, total XP, next level target, remaining XP needed, and completion percentage."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User level progress retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UserLevelProgressSuccessResponse",
                            summary = "Sample user level progress response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "userId": 2,
                                        "currentLevel": 2,
                                        "currentTitle": "Explorer",
                                        "totalXp": 150,
                                        "currentLevelRequiredXp": 100,
                                        "nextLevel": 3,
                                        "nextLevelTitle": "Challenger",
                                        "nextLevelRequiredXp": 300,
                                        "xpToNextLevel": 150,
                                        "progressPercent": 25.0
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "UserNotFoundResponse",
                            summary = "User not found response",
                            value = """
                                    {
                                      "success": false,
                                      "errors": [
                                        {
                                          "code": "USER_NOT_FOUND",
                                          "message": "User not found"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<UserLevelProgressResponse> getUserLevelProgress(
            @Parameter(description = "User ID to retrieve level progress for", example = "2", required = true)
            @PathVariable Long userId) {
        UserLevelProgressResponse response = levelService.getUserLevelProgress(userId);
        return ApiResponse.<UserLevelProgressResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/me/streaks")
    @Operation(
            summary = "Get current user learning streak and monthly check-in history",
            description = "Retrieves current streak, longest streak, today's check-in status, and list of check-in dates for a specified month for the authenticated user."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Streak details retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "MyStreakSuccessResponse",
                            summary = "Sample streak response with monthly check-ins",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "userId": 1,
                                        "currentStreak": 5,
                                        "longestStreak": 14,
                                        "lastActiveDate": "2026-11-16",
                                        "checkedInToday": true,
                                        "monthCheckinCount": 5,
                                        "checkinDates": [
                                          "2026-11-01",
                                          "2026-11-02",
                                          "2026-11-05",
                                          "2026-11-10",
                                          "2026-11-16"
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<StreakResponse> getMyStreak(
            @CurrentUserId Long userId,
            @Parameter(description = "Target year (2000-2100). Defaults to current year if null or out of range.", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Target month (1-12). Defaults to current month if null or out of range.", example = "11")
            @RequestParam(required = false) Integer month) {
        StreakResponse response = streakService.getUserStreak(userId, year, month);
        return ApiResponse.<StreakResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/{userId}/streaks")
    @Operation(
            summary = "Get user learning streak and monthly check-in history by ID",
            description = "Retrieves current streak, longest streak, today's check-in status, and list of check-in dates for a specified month for the specified user."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Streak details retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UserStreakSuccessResponse",
                            summary = "Sample streak response with monthly check-ins for specified user",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "userId": 2,
                                        "currentStreak": 5,
                                        "longestStreak": 14,
                                        "lastActiveDate": "2026-11-16",
                                        "checkedInToday": true,
                                        "monthCheckinCount": 5,
                                        "checkinDates": [
                                          "2026-11-01",
                                          "2026-11-02",
                                          "2026-11-05",
                                          "2026-11-10",
                                          "2026-11-16"
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "UserNotFoundResponse",
                            summary = "User not found response",
                            value = """
                                    {
                                      "success": false,
                                      "errors": [
                                        {
                                          "code": "USER_NOT_FOUND",
                                          "message": "User not found"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<StreakResponse> getUserStreak(
            @Parameter(description = "User ID to retrieve streak for", example = "2", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Target year (2000-2100). Defaults to current year if null or out of range.", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Target month (1-12). Defaults to current month if null or out of range.", example = "11")
            @RequestParam(required = false) Integer month) {
        StreakResponse response = streakService.getUserStreak(userId, year, month);
        return ApiResponse.<StreakResponse>builder()
                .success(true)
                .data(response)
                .build();
    }
}
