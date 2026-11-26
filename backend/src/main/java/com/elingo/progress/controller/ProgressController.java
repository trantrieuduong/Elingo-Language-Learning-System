package com.elingo.progress.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.progress.dto.response.DailyStudyProgressResponse;
import com.elingo.progress.dto.response.ProgressOverviewResponse;
import com.elingo.progress.dto.response.SkillProgressResponse;
import com.elingo.progress.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
@Tag(name = "Progress Controller")
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/overview")
    @Operation(summary = "Get overall learning progress summary for current user")
    public ApiResponse<ProgressOverviewResponse> getProgressOverview(@CurrentUserId Long userId) {
        return ApiResponse.<ProgressOverviewResponse>builder()
                .success(true)
                .data(progressService.getProgressOverview(userId))
                .build();
    }

    @GetMapping("/skills")
    @Operation(summary = "Get detailed learning progress breakdown by skill for current user")
    public ApiResponse<SkillProgressResponse> getSkillProgress(@CurrentUserId Long userId) {
        return ApiResponse.<SkillProgressResponse>builder()
                .success(true)
                .data(progressService.getSkillProgress(userId))
                .build();
    }

    @GetMapping("/daily")
    @Operation(summary = "Get daily study activity and XP progress for the last N days")
    public ApiResponse<List<DailyStudyProgressResponse>> getDailyStudyProgress(
            @RequestParam(required = false) Integer days,
            @CurrentUserId Long userId) {
        return ApiResponse.<List<DailyStudyProgressResponse>>builder()
                .success(true)
                .data(progressService.getDailyStudyProgress(userId, days))
                .build();
    }
}
