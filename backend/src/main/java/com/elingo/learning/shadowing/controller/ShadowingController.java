package com.elingo.learning.shadowing.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.learning.lesson.dto.response.SegmentSubmitResponse;
import com.elingo.learning.shadowing.dto.request.UpdateShadowingProgressRequest;
import com.elingo.learning.shadowing.service.ShadowingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
@Tag(name = "Shadowing Controller")
public class ShadowingController {

    private final ShadowingService shadowingService;

    @PatchMapping("/{lessonId}/segments/{segmentId}/shadowing-progress")
    @Operation(summary = "Submit shadowing result of a segment")
    public ApiResponse<SegmentSubmitResponse> updateProgress(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long lessonId,
            @PathVariable Long segmentId,
            @Valid @RequestBody UpdateShadowingProgressRequest request,
            @CurrentUserId Long userId) {
        SegmentSubmitResponse response = shadowingService.updateProgress(userId, lessonId, segmentId, request);
        return ApiResponse.<SegmentSubmitResponse>builder()
                .success(true)
                .data(response)
                .build();
    }
}