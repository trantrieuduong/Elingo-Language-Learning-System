package com.elingo.admin.content.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.content.service.AdminLessonService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.learning.lesson.dto.request.AdminLessonSearchRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminApiPaths.LESSONS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin - Lessons",
     description = "Management endpoints for lessons and lesson segments in the admin console")
public class AdminLessonController {

    private final AdminLessonService adminLessonService;

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
}
