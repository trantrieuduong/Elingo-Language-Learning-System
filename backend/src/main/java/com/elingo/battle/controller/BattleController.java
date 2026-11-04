package com.elingo.battle.controller;

import com.elingo.battle.dto.response.BattleHistoryItemResponse;
import com.elingo.battle.dto.response.BattleMatchDetailResponse;
import com.elingo.battle.dto.response.BattleStatsResponse;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.entity.BattleResult;
import com.elingo.battle.service.BattleHistoryService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/battles")
@RequiredArgsConstructor
@Tag(name = "Battle Controller", description = "Battle match history, stats, and detail APIs")
public class BattleController {

    private final BattleHistoryService battleHistoryService;

    @GetMapping("/history")
    @Operation(summary = "Get finished battle history of current user with optional result filter and pagination")
    public ApiResponse<PageResponse<BattleHistoryItemResponse>> history(
            @CurrentUserId Long userId,
            @RequestParam(required = false) BattleResult result,
            @RequestParam(required = false) BattleMode mode,
            @RequestParam(required = false) BattleMatchType matchType,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        PageResponse<BattleHistoryItemResponse> data = battleHistoryService.getHistory(userId, result, mode, matchType, pageable);
        return ApiResponse.<PageResponse<BattleHistoryItemResponse>>builder()
                .success(true)
                .data(data)
                .build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get battle win/loss/draw statistics of current user")
    public ApiResponse<BattleStatsResponse> stats(@CurrentUserId Long userId) {
        BattleStatsResponse data = battleHistoryService.getStats(userId);
        return ApiResponse.<BattleStatsResponse>builder()
                .success(true)
                .data(data)
                .build();
    }

    @GetMapping("/{matchId}")
    @Operation(summary = "Get round-by-round details of a finished battle match")
    public ApiResponse<BattleMatchDetailResponse> detail(
            @CurrentUserId Long userId,
            @PathVariable Long matchId
    ) {
        BattleMatchDetailResponse data = battleHistoryService.getMatchDetail(userId, matchId);
        return ApiResponse.<BattleMatchDetailResponse>builder()
                .success(true)
                .data(data)
                .build();
    }
}
