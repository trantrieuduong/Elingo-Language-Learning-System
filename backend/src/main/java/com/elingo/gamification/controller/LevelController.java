package com.elingo.gamification.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.gamification.dto.response.LevelResponse;
import com.elingo.gamification.dto.response.UserLevelProgressResponse;
import com.elingo.gamification.service.LevelService;
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
@RequestMapping("/gamification/levels")
@RequiredArgsConstructor
@Tag(name = "Level Controller", description = "Endpoints for gamification level milestones and user level progress")
public class LevelController {

    private final LevelService levelService;

    @GetMapping
    @Operation(
            summary = "Get all levels roadmap",
            description = "Retrieves the complete list of gamification levels configured in the system, ordered by required XP milestones."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Levels retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "LevelRoadmapSuccessResponse",
                            summary = "Sample level roadmap response",
                            value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "levelNo": 1,
                                          "requiredXp": 0,
                                          "title": "Novice"
                                        },
                                        {
                                          "levelNo": 2,
                                          "requiredXp": 100,
                                          "title": "Explorer"
                                        },
                                        {
                                          "levelNo": 3,
                                          "requiredXp": 300,
                                          "title": "Challenger"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<List<LevelResponse>> getAllLevels() {
        List<LevelResponse> response = levelService.getAllLevels();
        return ApiResponse.<List<LevelResponse>>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/progress")
    @Operation(
            summary = "Get user level progress",
            description = "Retrieves level progression details for the specified user ID (or the current authenticated user if userId parameter is omitted), including current level, total XP, next level target, remaining XP needed, and completion percentage."
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
            @CurrentUserId Long currentUserId,

            @Parameter(description = "Optional user ID to inspect. Defaults to current user if not provided.", example = "1")
            @RequestParam(required = false) Long userId) {

        Long targetUserId = (userId != null) ? userId : currentUserId;
        UserLevelProgressResponse response = levelService.getUserLevelProgress(targetUserId);
        return ApiResponse.<UserLevelProgressResponse>builder()
                .success(true)
                .data(response)
                .build();
    }
}
