package com.elingo.gamification.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.gamification.dto.response.StreakResponse;
import com.elingo.gamification.service.StreakService;
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

@RestController
@RequestMapping("/gamification/streaks")
@RequiredArgsConstructor
@Tag(name = "Streak Controller", description = "Endpoints for user learning streaks and daily check-in tracking")
public class StreakController {

    private final StreakService streakService;

    @GetMapping
    @Operation(
            summary = "Get user learning streak and check-in history",
            description = "Retrieves current streak, longest streak, today's check-in status, and list of check-in dates for a specified month. If userId is omitted, retrieves streak for the current authenticated user."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Streak details retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "StreakSuccessResponse",
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
            @CurrentUserId Long currentUserId,
            @Parameter(description = "Optional user ID to inspect. Defaults to current user if not provided.", example = "1")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Target year (2000-2100). Defaults to current year if null or out of range.", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Target month (1-12). Defaults to current month if null or out of range.", example = "11")
            @RequestParam(required = false) Integer month) {

        StreakResponse response;
        if (userId != null) response = streakService.getUserStreak(userId, year, month);
        else response = streakService.getUserStreak(currentUserId, year, month);

        return ApiResponse.<StreakResponse>builder()
                .success(true)
                .data(response)
                .build();
    }
}
