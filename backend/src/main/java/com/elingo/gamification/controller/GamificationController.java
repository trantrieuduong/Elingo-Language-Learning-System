package com.elingo.gamification.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.gamification.dto.response.LeaderboardItemResponse;
import com.elingo.gamification.dto.response.LeaderboardResponse;
import com.elingo.gamification.dto.response.LevelResponse;
import com.elingo.gamification.entity.LeaderboardPeriodType;
import com.elingo.gamification.service.LeaderboardService;
import com.elingo.gamification.service.LevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/gamification")
@RequiredArgsConstructor
@Tag(
        name = "Gamification Controller",
        description = "Endpoints for system-wide gamification features: level roadmaps and global leaderboards"
)
public class GamificationController {

    private final LevelService levelService;
    private final LeaderboardService leaderboardService;

    @GetMapping("/levels")
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

    @GetMapping("/leaderboards")
    @Operation(
            summary = "Get realtime leaderboard",
            description = "Retrieves the current active leaderboard (weekly or monthly) computed in realtime from Redis, including paginated user rankings and the authenticated user's current rank."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Realtime leaderboard retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "RealtimeLeaderboardSuccessResponse",
                            summary = "Sample realtime leaderboard response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "periodType": "WEEKLY",
                                        "periodKey": "2026-W46",
                                        "leaderboard": {
                                          "content": [
                                            {
                                              "rank": 1,
                                              "userId": 10,
                                              "username": "alex_master",
                                              "avatarUrl": "https://cdn.elingo.com/avatars/user10.png",
                                              "totalXp": 1250
                                            },
                                            {
                                              "rank": 2,
                                              "userId": 25,
                                              "username": "sarah_lang",
                                              "avatarUrl": "https://cdn.elingo.com/avatars/user25.png",
                                              "totalXp": 1100
                                            }
                                          ],
                                          "page": 1,
                                          "size": 20,
                                          "totalElements": 128,
                                          "totalPages": 7,
                                          "isLast": false
                                        },
                                        "myRank": {
                                          "rank": 5,
                                          "totalXp": 850,
                                          "diffToNextRank": 50,
                                          "periodType": "WEEKLY",
                                          "periodKey": "2026-W46"
                                        }
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<LeaderboardResponse> getRealtimeLeaderboard(
            @CurrentUserId
            Long currentUserId,

            @Parameter(description = "Leaderboard time period (WEEKLY or MONTHLY). Defaults to WEEKLY.", example = "WEEKLY")
            @RequestParam(defaultValue = "WEEKLY")
            LeaderboardPeriodType periodType,

            @PageableDefault(size = 20)
            Pageable pageable) {

        LeaderboardResponse response = leaderboardService.getRealtimeLeaderboard(currentUserId, periodType, pageable);
        return ApiResponse.<LeaderboardResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/leaderboards/history")
    @Operation(
            summary = "Get historical leaderboard snapshot",
            description = "Retrieves archived leaderboard rankings from persistent database snapshots for a finalized past week or month."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Historical leaderboard page retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "HistoricalLeaderboardSuccessResponse",
                            summary = "Sample historical leaderboard response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "content": [
                                          {
                                            "rank": 1,
                                            "userId": 10,
                                            "username": "alex_master",
                                            "avatarUrl": "https://cdn.elingo.com/avatars/user10.png",
                                            "totalXp": 2500
                                          },
                                          {
                                            "rank": 2,
                                            "userId": 18,
                                            "username": "david_v",
                                            "avatarUrl": "https://cdn.elingo.com/avatars/user18.png",
                                            "totalXp": 2350
                                          }
                                        ],
                                        "page": 1,
                                        "size": 20,
                                        "totalElements": 95,
                                        "totalPages": 5,
                                        "isLast": false
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<PageResponse<LeaderboardItemResponse>> getHistoricalLeaderboard(
            @Parameter(description = "Leaderboard period type (WEEKLY or MONTHLY)", example = "WEEKLY", required = true)
            @RequestParam
            LeaderboardPeriodType periodType,

            @Parameter(description = "Archived period key (e.g., '2026-W45' for weekly, '2026-10' for monthly)", example = "2026-W45", required = true)
            @RequestParam
            String periodKey,

            @PageableDefault(size = 20)
            Pageable pageable) {

        PageResponse<LeaderboardItemResponse> response = leaderboardService.getHistoricalLeaderboard(
                periodType,
                periodKey,
                pageable
        );
        return ApiResponse.<PageResponse<LeaderboardItemResponse>>builder()
                .success(true)
                .data(response)
                .build();
    }
}
