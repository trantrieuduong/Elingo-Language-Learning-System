package com.elingo.gamification.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.gamification.dto.response.UserBadgeEarnedResponse;
import com.elingo.gamification.service.BadgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/gamification/badges")
@RequiredArgsConstructor
@Tag(name = "Badge Controller", description = "Endpoints for user badges and achievement tracking")
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping
    @Operation(
            summary = "Get earned badges of a user",
            description = "Retrieves the list of badges earned by the specified user, ordered by most recent earned date. If userId is omitted, retrieves badges for the current authenticated user."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Earned badges retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "BadgeListSuccessResponse",
                            summary = "Sample list of earned badges",
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
                                        },
                                        {
                                          "id": 2,
                                          "code": "TOTAL_XP_1000",
                                          "name": "XP Novice",
                                          "description": "Accumulate 1,000 total experience points",
                                          "iconUrl": "https://cdn.elingo.com/badges/xp-1000.png",
                                          "conditionType": "TOTAL_XP",
                                          "conditionValue": 1000,
                                          "earnedAt": "2026-11-15T18:20:00"
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
            @CurrentUserId Long currentUserId,
            @Parameter(description = "Optional user ID to inspect. Defaults to current user if not provided.", example = "1")
            @RequestParam(required = false) Long userId) {

        List<UserBadgeEarnedResponse> response;
        if (userId != null) response = badgeService.getUserBadges(userId);
        else response = badgeService.getUserBadges(currentUserId);

        return ApiResponse.<List<UserBadgeEarnedResponse>>builder()
                .success(true)
                .data(response)
                .build();
    }
}
