package com.elingo.admin.gamification.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.gamification.dto.request.LevelRequest;
import com.elingo.gamification.dto.response.AdminLevelResponse;
import com.elingo.admin.gamification.service.AdminLevelService;
import com.elingo.common.dto.ApiResponse;
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

import java.util.List;

@RestController
@RequestMapping(AdminApiPaths.GAMIFICATION_LEVELS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Levels",
        description = "Management endpoints for level XP thresholds in the admin console"
)
public class AdminLevelController {

    private final AdminLevelService adminLevelService;

    @GetMapping
    @Operation(summary = "Get all levels", description = "Retrieves all level thresholds sorted by level number")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "List of level milestones retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "GetAllLevelsSuccessResponse",
                                    summary = "Sample level list response",
                                    value = """
                                            {
                                              "success": true,
                                              "data": [
                                                {
                                                  "levelNo": 1,
                                                  "requiredXp": 0,
                                                  "title": "Beginner",
                                                  "createdAt": "2026-11-21T10:00:00",
                                                  "updatedAt": "2026-11-21T10:00:00"
                                                },
                                                {
                                                  "levelNo": 2,
                                                  "requiredXp": 100,
                                                  "title": "Novice Explorer",
                                                  "createdAt": "2026-11-21T10:00:00",
                                                  "updatedAt": "2026-11-21T10:00:00"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Authentication token is missing or expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    public ApiResponse<List<AdminLevelResponse>> getAllLevels() {
        return ApiResponse.<List<AdminLevelResponse>>builder()
                .success(true)
                .data(adminLevelService.getAllLevels())
                .build();
    }

    @GetMapping("/{levelNo}")
    @Operation(summary = "Get level by number", description = "Retrieves a specific level threshold by level number")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "Level milestone retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "GetLevelByNoSuccessResponse",
                                    summary = "Sample level details response",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "levelNo": 2,
                                                "requiredXp": 100,
                                                "title": "Novice Explorer",
                                                "createdAt": "2026-11-21T10:00:00",
                                                "updatedAt": "2026-11-21T10:00:00"
                                              }
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Invalid level number")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Authentication token is missing or expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found - Level milestone does not exist")
    public ApiResponse<AdminLevelResponse> getLevelByNo(
            @Parameter(description = "Sequential level number", example = "2")
            @PathVariable @Positive(message = "INVALID_REQUEST") Short levelNo) {
        return ApiResponse.<AdminLevelResponse>builder()
                .success(true)
                .data(adminLevelService.getLevelByNo(levelNo))
                .build();
    }

    @PostMapping
    @Operation(summary = "Create a new level", description = "Creates a new level threshold (level number is automatically assigned)")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Level milestone creation payload",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = LevelRequest.class),
                    examples = @ExampleObject(
                            name = "CreateLevelRequestExample",
                            summary = "Sample level creation payload",
                            value = """
                                    {
                                      "requiredXp": 150,
                                      "title": "Intermediate Explorer"
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "Level milestone created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "CreateLevelSuccessResponse",
                                    summary = "Sample created level response",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "levelNo": 3,
                                                "requiredXp": 150,
                                                "title": "Intermediate Explorer",
                                                "createdAt": "2026-11-21T10:00:00",
                                                "updatedAt": "2026-11-21T10:00:00"
                                              }
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Invalid payload or required XP violates ascending order")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Authentication token is missing or expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    public ApiResponse<AdminLevelResponse> createLevel(
            @Valid @RequestBody LevelRequest request) {
        return ApiResponse.<AdminLevelResponse>builder()
                .success(true)
                .data(adminLevelService.createLevel(request))
                .build();
    }

    @PutMapping("/{levelNo}")
    @Operation(summary = "Update level", description = "Updates a level threshold. Default level 1 required XP cannot be changed")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Level milestone update payload",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = LevelRequest.class),
                    examples = @ExampleObject(
                            name = "UpdateLevelRequestExample",
                            summary = "Sample level update payload",
                            value = """
                                    {
                                      "requiredXp": 160,
                                      "title": "Advanced Explorer"
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "Level milestone updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "UpdateLevelSuccessResponse",
                                    summary = "Sample updated level response",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "levelNo": 2,
                                                "requiredXp": 160,
                                                "title": "Advanced Explorer",
                                                "createdAt": "2026-11-21T10:00:00",
                                                "updatedAt": "2026-11-21T12:00:00"
                                              }
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Invalid payload, modifying default level 1, or XP order violation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Authentication token is missing or expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found - Level milestone does not exist")
    public ApiResponse<AdminLevelResponse> updateLevel(
            @Parameter(description = "Sequential level number to update", example = "2")
            @PathVariable @Positive(message = "INVALID_REQUEST") Short levelNo,
            @Valid @RequestBody LevelRequest request) {
        return ApiResponse.<AdminLevelResponse>builder()
                .success(true)
                .data(adminLevelService.updateLevel(levelNo, request))
                .build();
    }

    @DeleteMapping("/{levelNo}")
    @Operation(summary = "Delete level", description = "Deletes a level threshold. Default level 1 cannot be deleted")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "Level milestone deleted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "DeleteLevelSuccessResponse",
                                    summary = "Sample delete level response",
                                    value = """
                                            {
                                              "success": true
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Default level 1 cannot be deleted")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Authentication token is missing or expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found - Level milestone does not exist")
    public ApiResponse<Void> deleteLevel(
            @Parameter(description = "Sequential level number to delete", example = "2")
            @PathVariable @Positive(message = "INVALID_REQUEST") Short levelNo) {
        adminLevelService.deleteLevel(levelNo);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
