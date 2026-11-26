package com.elingo.admin.content.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.content.service.AdminLessonSegmentService;
import com.elingo.admin.content.service.AdminLessonService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.learning.lesson.dto.request.AdminLessonSearchRequest;
import com.elingo.learning.lesson.dto.request.DeleteLessonSegmentsRequest;
import com.elingo.learning.lesson.dto.request.LessonRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonResponse;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AdminApiPaths.LESSONS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Lessons",
     description = "Management endpoints for lessons and lesson segments in the admin console")
public class AdminLessonController {

    private final AdminLessonService adminLessonService;
    private final AdminLessonSegmentService adminLessonSegmentService;

    @GetMapping
    @Operation(summary = "Search and filter lessons",
               description = "Retrieves a paginated list of lessons of any status, filtered by keyword, status, premium flag, tags and CEFR levels. Sortable by id, createdAt, title, publishedAt.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Paginated list of lessons retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "SearchLessonsSuccessResponse",
                            summary = "Sample paginated lesson list response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "content": [
                                          {
                                            "id": 7,
                                            "title": "How to Order Coffee in English",
                                            "slug": "how-to-order-coffee-in-english-3fa9c1d2",
                                            "description": "A short everyday conversation.",
                                            "isPremium": false,
                                            "status": "DRAFT",
                                            "durationMs": 212000,
                                            "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                                            "thumbnailUrl": "https://cdn.elingo.pro/system/lessons/5f1c.png",
                                            "publishedAt": null,
                                            "segmentCount": 12,
                                            "createdAt": "2026-11-20T09:00:00",
                                            "updatedAt": "2026-11-21T10:30:00",
                                            "tags": [],
                                            "cefrLevels": []
                                          }
                                        ],
                                        "page": 1,
                                        "size": 20,
                                        "totalElements": 1,
                                        "totalPages": 1,
                                        "isLast": true
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request — KEYWORD_INVALID, INVALID_SORT",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "KeywordInvalidResponse",
                                    summary = "Keyword exceeds max length",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "KEYWORD_INVALID",
                                                "message": "Keyword must not exceed 255 characters"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "InvalidSortResponse",
                                    summary = "Sort field not allowed",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "INVALID_SORT",
                                                "message": "Invalid sort field"
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
    public ApiResponse<PageResponse<AdminLessonResponse>> searchLessons(
            @Valid @ParameterObject AdminLessonSearchRequest request,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<AdminLessonResponse>>builder()
                .success(true)
                .data(adminLessonService.searchLessons(request, pageable))
                .build();
    }

    @GetMapping("/{lessonId}")
    @Operation(summary = "Get lesson by ID", description = "Retrieves one lesson of any status, including segment count.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetLessonByIdSuccessResponse",
                            summary = "Sample lesson detail response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 7,
                                        "title": "How to Order Coffee in English",
                                        "slug": "how-to-order-coffee-in-english-3fa9c1d2",
                                        "description": "A short everyday conversation.",
                                        "isPremium": false,
                                        "status": "PUBLISHED",
                                        "durationMs": 212000,
                                        "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                                        "thumbnailUrl": "https://cdn.elingo.pro/system/lessons/5f1c.png",
                                        "publishedAt": "2026-11-21T10:30:00",
                                        "segmentCount": 12,
                                        "createdAt": "2026-11-20T09:00:00",
                                        "updatedAt": "2026-11-21T10:30:00",
                                        "tags": [],
                                        "cefrLevels": []
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request — lessonId must be a positive integer",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "InvalidLessonIdResponse",
                            summary = "LessonId validation failure",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "INVALID_REQUEST",
                                        "message": "Invalid request parameters or body"
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
            description = "Not Found — LESSON_NOT_FOUND",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "LessonNotFoundResponse",
                            summary = "Lesson not found error",
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
    public ApiResponse<AdminLessonResponse> getLessonById(
            @Parameter(description = "Identifier of the lesson", example = "7", required = true)
            @PathVariable @Positive Long lessonId) {
        return ApiResponse.<AdminLessonResponse>builder()
                .success(true)
                .data(adminLessonService.getLessonById(lessonId))
                .build();
    }

    @PostMapping
    @Operation(summary = "Create a lesson",
               description = "Creates a new lesson in DRAFT status. Writes audit action LESSON_CREATE.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Lesson data to create",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LessonRequest.class)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "CreateLessonSuccessResponse",
                            summary = "Sample create lesson response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 7,
                                        "title": "How to Order Coffee in English",
                                        "slug": "how-to-order-coffee-in-english-3fa9c1d2",
                                        "description": "A short everyday conversation.",
                                        "isPremium": false,
                                        "status": "DRAFT",
                                        "durationMs": 212000,
                                        "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                                        "thumbnailUrl": null,
                                        "publishedAt": null,
                                        "segmentCount": 0,
                                        "createdAt": "2026-11-20T09:00:00",
                                        "updatedAt": "2026-11-20T09:00:00",
                                        "tags": [],
                                        "cefrLevels": []
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request — INVALID_REQUEST, SOURCE_URL_INVALID, INVALID_FILE_TYPE",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "InvalidRequestResponse",
                                    summary = "Missing required field",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "INVALID_REQUEST",
                                                "message": "Invalid request parameters or body"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "SourceUrlInvalidResponse",
                                    summary = "Invalid YouTube URL",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "SOURCE_URL_INVALID",
                                                "message": "Source url must be a valid YouTube link"
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
            description = "Not Found — TAG_NOT_FOUND, CEFR_LEVEL_NOT_FOUND",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "TagNotFoundResponse",
                            summary = "Tag not found error",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "TAG_NOT_FOUND",
                                        "message": "Tag not found"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<AdminLessonResponse> createLesson(
            @CurrentUserId Long adminId,
            @Valid @RequestBody LessonRequest request) {
        return ApiResponse.<AdminLessonResponse>builder()
                .success(true)
                .data(adminLessonService.createLesson(adminId, request))
                .build();
    }

    @PutMapping("/{lessonId}")
    @Operation(summary = "Update a lesson (full replace)",
               description = "Fully replaces all editable fields of a lesson. slug/status/publishedAt are preserved. Writes audit action LESSON_UPDATE.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Complete lesson data to replace with",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LessonRequest.class)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson updated successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UpdateLessonSuccessResponse",
                            summary = "Sample updated lesson response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 7,
                                        "title": "Updated Title",
                                        "slug": "how-to-order-coffee-in-english-3fa9c1d2",
                                        "description": null,
                                        "isPremium": true,
                                        "status": "PUBLISHED",
                                        "durationMs": 300000,
                                        "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                                        "thumbnailUrl": null,
                                        "publishedAt": "2026-11-21T10:30:00",
                                        "segmentCount": 5,
                                        "createdAt": "2026-11-20T09:00:00",
                                        "updatedAt": "2026-11-25T14:00:00",
                                        "tags": [],
                                        "cefrLevels": []
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request — INVALID_REQUEST, SOURCE_URL_INVALID, INVALID_FILE_TYPE",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "SourceUrlInvalidResponse",
                            summary = "Invalid YouTube URL",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "SOURCE_URL_INVALID",
                                        "message": "Source url must be a valid YouTube link"
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
            description = "Not Found — LESSON_NOT_FOUND, TAG_NOT_FOUND, CEFR_LEVEL_NOT_FOUND",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "LessonNotFoundResponse",
                            summary = "Lesson not found error",
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
    public ApiResponse<AdminLessonResponse> updateLesson(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the lesson", example = "7", required = true)
            @PathVariable @Positive Long lessonId,
            @Valid @RequestBody LessonRequest request) {
        return ApiResponse.<AdminLessonResponse>builder()
                .success(true)
                .data(adminLessonService.updateLesson(adminId, lessonId, request))
                .build();
    }

    @PutMapping("/{lessonId}/publish")
    @Operation(summary = "Publish a lesson",
               description = "Transitions the lesson to PUBLISHED status. Requires at least 1 segment. publishedAt is set only on first publish. Writes audit action LESSON_PUBLISH.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson published successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "PublishLessonSuccessResponse",
                            summary = "Sample published lesson response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 7,
                                        "status": "PUBLISHED",
                                        "publishedAt": "2026-11-25T14:00:00",
                                        "segmentCount": 5
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request — LESSON_PUBLISHED_FAILED (no segments)",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "LessonPublishedFailedResponse",
                            summary = "Lesson has no segments",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "LESSON_PUBLISHED_FAILED",
                                        "message": "Cannot publish a lesson without segments"
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
    public ApiResponse<AdminLessonResponse> publishLesson(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the lesson to publish", example = "7", required = true)
            @PathVariable @Positive Long lessonId) {
        return ApiResponse.<AdminLessonResponse>builder()
                .success(true)
                .data(adminLessonService.publishLesson(adminId, lessonId))
                .build();
    }

    @PutMapping("/{lessonId}/unpublish")
    @Operation(summary = "Unpublish a lesson (back to DRAFT)",
               description = "Transitions the lesson to DRAFT status (also restores from ARCHIVED). publishedAt is cleared. Writes audit action LESSON_UNPUBLISH.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson unpublished successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UnpublishLessonSuccessResponse",
                            summary = "Sample unpublished lesson response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 7,
                                        "status": "DRAFT",
                                        "publishedAt": null
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
    public ApiResponse<AdminLessonResponse> unpublishLesson(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the lesson to unpublish", example = "7", required = true)
            @PathVariable @Positive Long lessonId) {
        return ApiResponse.<AdminLessonResponse>builder()
                .success(true)
                .data(adminLessonService.unpublishLesson(adminId, lessonId))
                .build();
    }

    @PutMapping("/{lessonId}/archive")
    @Operation(summary = "Archive a lesson",
               description = "Transitions the lesson to ARCHIVED status. publishedAt is preserved. Learners can no longer see the lesson. Writes audit action LESSON_ARCHIVE.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson archived successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "ArchiveLessonSuccessResponse",
                            summary = "Sample archived lesson response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 7,
                                        "status": "ARCHIVED",
                                        "publishedAt": "2026-11-21T10:30:00"
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
    public ApiResponse<AdminLessonResponse> archiveLesson(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the lesson to archive", example = "7", required = true)
            @PathVariable @Positive Long lessonId) {
        return ApiResponse.<AdminLessonResponse>builder()
                .success(true)
                .data(adminLessonService.archiveLesson(adminId, lessonId))
                .build();
    }

    @GetMapping("/{lessonId}/segments")
    @Operation(summary = "Get segments of a lesson",
               description = "Returns all segments of a lesson ordered by startMs ascending. Admin can view segments of any lesson regardless of status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Segments retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
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
    public ApiResponse<java.util.List<AdminLessonSegmentResponse>> getSegmentsByLessonId(
            @Parameter(description = "Identifier of the lesson", example = "7", required = true)
            @PathVariable @Positive Long lessonId) {
        return ApiResponse.<java.util.List<AdminLessonSegmentResponse>>builder()
                .success(true)
                .data(adminLessonSegmentService.getSegmentsByLessonId(lessonId))
                .build();
    }

    @DeleteMapping("/{lessonId}/segments")
    @Operation(summary = "Delete one or more segments of a lesson",
               description = "All-or-nothing. Send a single id in segmentIds to delete one segment. A published lesson must keep at least one segment. Also deletes learners' segment progress (segment_word_accuracy, user_segment_progress). Writes audit action SEGMENT_DELETE.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "IDs of segments to delete",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DeleteLessonSegmentsRequest.class)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Segments deleted successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "DeleteSegmentsSuccessResponse",
                            summary = "Bulk delete success",
                            value = """
                                    {
                                      "success": true,
                                      "data": null
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request — INVALID_REQUEST, LESSON_SEGMENT_REQUIRED",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "InvalidRequestResponse",
                                    summary = "segmentIds empty or invalid",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "INVALID_REQUEST",
                                                "message": "Invalid request parameters or body"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "LessonSegmentRequiredResponse",
                                    summary = "Cannot delete last segment of a published lesson",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "LESSON_SEGMENT_REQUIRED",
                                                "message": "A published lesson must have at least one segment"
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
            description = "Not Found — LESSON_NOT_FOUND, LESSON_SEGMENT_NOT_FOUND",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "NotFoundResponse",
                            summary = "Lesson or segment not found",
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
    public ApiResponse<Void> deleteSegments(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the lesson", example = "7", required = true)
            @PathVariable @Positive Long lessonId,
            @Valid @RequestBody DeleteLessonSegmentsRequest request) {
        adminLessonSegmentService.deleteSegments(adminId, lessonId, request);
        return ApiResponse.<Void>builder().success(true).build();
    }
}
