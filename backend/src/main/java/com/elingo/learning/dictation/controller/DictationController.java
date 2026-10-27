package com.elingo.learning.dictation.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.learning.dictation.dto.request.UpdateDictationProgressRequest;
import com.elingo.learning.dictation.service.DictationService;
import com.elingo.learning.lesson.dto.response.SegmentSubmitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
@Tag(name = "Dictation Controller")
public class DictationController {

    private final DictationService dictationService;

    @PatchMapping("/{lessonId}/segments/{segmentId}/dictation-progress")
    @Operation(summary = "Submit dictation result of a segment")
    public ApiResponse<SegmentSubmitResponse> updateProgress(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long lessonId,
            @PathVariable Long segmentId,
            @Valid @RequestBody UpdateDictationProgressRequest request,
            @CurrentUserId Long userId) {
        SegmentSubmitResponse response = dictationService.updateProgress(userId, lessonId, segmentId, request);
        return ApiResponse.<SegmentSubmitResponse>builder()
                .success(true)
                .data(response)
                .build();
    }
}