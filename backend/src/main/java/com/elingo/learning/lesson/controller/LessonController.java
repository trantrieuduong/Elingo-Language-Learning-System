package com.elingo.learning.lesson.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.learning.lesson.dto.request.GetPublishedLessonsRequest;
import com.elingo.learning.lesson.dto.response.LessonDetailResponse;
import com.elingo.learning.lesson.dto.response.LessonResponse;
import com.elingo.learning.lesson.dto.response.LessonSegmentResponse;
import com.elingo.learning.lesson.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
@Tag(name = "Lesson Controller")
public class LessonController {

    private final LessonService lessonService;

    @GetMapping
    @Operation(summary = "Get all published lessons with optional filters (cefrLevelIds, tagIds, keyword) and pagination (9 items/page)")
    public ApiResponse<PageResponse<LessonResponse>> getAllPublishedLessons(
            @ModelAttribute @Valid GetPublishedLessonsRequest request,
            @CurrentUserId Long userId) {
        PageResponse<LessonResponse> response = lessonService.getAllPublishedLessons(userId, request);
        return ApiResponse.<PageResponse<LessonResponse>>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get lesson detail by slug (requires PUBLISHED status)")
    public ApiResponse<LessonDetailResponse> getLessonBySlug(
            @PathVariable String slug,
            @CurrentUserId Long userId) {
        LessonDetailResponse response = lessonService.getLessonBySlug(userId, slug);
        return ApiResponse.<LessonDetailResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/{slug}/segments")
    @Operation(summary = "Get all segments of a lesson ordered by startMs ascending")
    public ApiResponse<List<LessonSegmentResponse>> getSegmentsByLesson(
            @PathVariable String slug,
            @CurrentUserId Long userId) {
        List<LessonSegmentResponse> response = lessonService.getSegmentsByLesson(userId, slug);
        return ApiResponse.<List<LessonSegmentResponse>>builder()
                .success(true)
                .data(response)
                .build();
    }
}
