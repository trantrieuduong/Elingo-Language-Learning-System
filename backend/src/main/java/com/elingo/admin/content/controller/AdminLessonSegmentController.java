package com.elingo.admin.content.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.content.service.AdminLessonSegmentService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.learning.lesson.dto.request.CreateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.request.UpdateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonSegmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AdminApiPaths.LESSON_SEGMENTS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Lesson Segments",
     description = "Management endpoints for lesson segments in the admin console")
public class AdminLessonSegmentController {

    private final AdminLessonSegmentService adminLessonSegmentService;

    @PostMapping
    @Operation(summary = "Create a lesson segment",
               description = "Creates a new segment for a lesson. Validates time range, overlap, and normalizes the transcript. Writes audit action SEGMENT_CREATE.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Segment data to create",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateLessonSegmentRequest.class)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Segment created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "CreateSegmentSuccessResponse",
                            summary = "Sample create segment response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 11,
                                        "lessonId": 7,
                                        "startMs": 1500,
                                        "endMs": 6200,
                                        "transcriptOriginal": "Hi, can I get a large coffee, please?",
                                        "transcriptNormalized": "hi can i get a large coffee please",
                                        "translation": "Xin chào, cho tôi một ly cà phê lớn nhé?",
                                        "createdAt": "2026-11-20T09:05:00",
                                        "updatedAt": "2026-11-20T09:05:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request — INVALID_REQUEST, INVALID_SEGMENT_TIME_RANGE, SEGMENT_EXCEEDS_DURATION, SEGMENT_TIME_OVERLAP",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "InvalidTimeRangeResponse",
                                    summary = "startMs >= endMs",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "INVALID_SEGMENT_TIME_RANGE",
                                                "message": "Segment start time must be less than end time"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "ExceedsDurationResponse",
                                    summary = "endMs exceeds lesson duration",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "SEGMENT_EXCEEDS_DURATION",
                                                "message": "Segment end time exceeds lesson duration"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "TimeOverlapResponse",
                                    summary = "Segment overlaps with existing segment",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "SEGMENT_TIME_OVERLAP",
                                                "message": "Segment time range overlaps with an existing segment"
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found — LESSON_NOT_FOUND",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "LessonNotFoundResponse",
                            summary = "Lesson not found",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "LESSON_NOT_FOUND",
                                        "message": "Lesson not found"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<AdminLessonSegmentResponse> createSegment(
            @CurrentUserId Long adminId,
            @Valid @RequestBody CreateLessonSegmentRequest request) {
        return ApiResponse.<AdminLessonSegmentResponse>builder()
                .success(true)
                .data(adminLessonSegmentService.createSegment(adminId, request))
                .build();
    }

    @PutMapping("/{segmentId}")
    @Operation(summary = "Update a lesson segment (full replace)",
               description = "Fully replaces all fields of a segment. Validates time range and overlap. transcriptNormalized is regenerated from transcriptOriginal. Writes audit action SEGMENT_UPDATE.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Complete segment data to replace with",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdateLessonSegmentRequest.class)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Segment updated successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UpdateSegmentSuccessResponse",
                            summary = "Sample updated segment response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 11,
                                        "lessonId": 7,
                                        "startMs": 2000,
                                        "endMs": 7000,
                                        "transcriptOriginal": "Updated transcript text.",
                                        "transcriptNormalized": "updated transcript text",
                                        "translation": "Bản dịch cập nhật.",
                                        "createdAt": "2026-11-20T09:05:00",
                                        "updatedAt": "2026-11-26T10:00:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request — INVALID_REQUEST, INVALID_SEGMENT_TIME_RANGE, SEGMENT_EXCEEDS_DURATION, SEGMENT_TIME_OVERLAP",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "TimeOverlapResponse",
                            summary = "Time overlaps with another segment",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "SEGMENT_TIME_OVERLAP",
                                        "message": "Segment time range overlaps with an existing segment"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found — LESSON_SEGMENT_NOT_FOUND",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "SegmentNotFoundResponse",
                            summary = "Segment not found",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "LESSON_SEGMENT_NOT_FOUND",
                                        "message": "Lesson segment not found"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<AdminLessonSegmentResponse> updateSegment(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the segment to update", example = "11", required = true)
            @PathVariable @Positive Long segmentId,
            @Valid @RequestBody UpdateLessonSegmentRequest request) {
        return ApiResponse.<AdminLessonSegmentResponse>builder()
                .success(true)
                .data(adminLessonSegmentService.updateSegment(adminId, segmentId, request))
                .build();
    }

}
