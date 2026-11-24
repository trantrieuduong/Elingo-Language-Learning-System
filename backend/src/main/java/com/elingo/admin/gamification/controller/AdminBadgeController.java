package com.elingo.admin.gamification.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.gamification.dto.request.BadgeRequest;
import com.elingo.gamification.dto.response.AdminBadgeResponse;
import com.elingo.admin.gamification.service.AdminBadgeService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.gamification.entity.BadgeConditionType;
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
@RequestMapping(AdminApiPaths.GAMIFICATION_BADGES)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Badges",
        description = "Management endpoints for badges in the admin console"
)
public class AdminBadgeController {

    private final AdminBadgeService adminBadgeService;

    @GetMapping
    @Operation(summary = "Get all badges", description = "Retrieves all badges, optionally filtered by condition type")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "List of badges retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "GetAllBadgesSuccessResponse",
                                    summary = "Sample badge list response",
                                    value = """
                                            {
                                              "success": true,
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "code": "streak-7-days",
                                                  "name": "7-Day Streak Master",
                                                  "description": "Maintain a learning streak for 7 consecutive days",
                                                  "iconUrl": "https://cdn.elingo.com/gamification/badges/streak-7-days.png",
                                                  "conditionType": "STREAK_DAYS",
                                                  "conditionValue": 7,
                                                  "createdAt": "2026-11-21T10:00:00"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Authentication token is missing or expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    public ApiResponse<List<AdminBadgeResponse>> getAllBadges(
            @Parameter(description = "Filter badges by condition type", example = "STREAK_DAYS")
            @RequestParam(required = false) BadgeConditionType conditionType) {
        return ApiResponse.<List<AdminBadgeResponse>>builder()
                .success(true)
                .data(adminBadgeService.getAllBadges(conditionType))
                .build();
    }

    @GetMapping("/condition-types")
    @Operation(summary = "Get all badge condition types", description = "Retrieves all supported badge condition types for filtering and dropdown options")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "List of supported badge condition types retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "GetConditionTypesSuccessResponse",
                                    summary = "Sample condition type list response",
                                    value = """
                                            {
                                              "success": true,
                                              "data": [
                                                "STREAK_DAYS",
                                                "CARDS_LEARNED",
                                                "DICTATION_COMPLETED",
                                                "SHADOWING_COMPLETED",
                                                "BATTLE_WON",
                                                "LEVEL_REACHED"
                                              ]
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Authentication token is missing or expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    public ApiResponse<List<BadgeConditionType>> getConditionTypes() {
        return ApiResponse.<List<BadgeConditionType>>builder()
                .success(true)
                .data(adminBadgeService.getConditionTypes())
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get badge by ID", description = "Retrieves a specific badge by its ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "Badge retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "GetBadgeByIdSuccessResponse",
                                    summary = "Sample badge details response",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "id": 1,
                                                "code": "streak-7-days",
                                                "name": "7-Day Streak Master",
                                                "description": "Maintain a learning streak for 7 consecutive days",
                                                "iconUrl": "https://cdn.elingo.com/gamification/badges/streak-7-days.png",
                                                "conditionType": "STREAK_DAYS",
                                                "conditionValue": 7,
                                                "createdAt": "2026-11-21T10:00:00"
                                              }
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Invalid badge ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Authentication token is missing or expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found - Badge does not exist")
    public ApiResponse<AdminBadgeResponse> getBadgeById(
            @Parameter(description = "Unique ID of the badge", example = "1")
            @PathVariable @Positive(message = "INVALID_REQUEST") Long id) {
        return ApiResponse.<AdminBadgeResponse>builder()
                .success(true)
                .data(adminBadgeService.getBadgeById(id))
                .build();
    }

    @PostMapping
    @Operation(summary = "Create badge", description = "Creates a new badge with unique code")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Badge creation payload",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = BadgeRequest.class),
                    examples = @ExampleObject(
                            name = "CreateBadgeRequestExample",
                            summary = "Sample badge creation payload",
                            value = """
                                    {
                                      "code": "streak-7-days",
                                      "name": "7-Day Streak Master",
                                      "description": "Maintain a learning streak for 7 consecutive days",
                                      "iconUrl": "gamification/badges/streak-7-days.png",
                                      "conditionType": "STREAK_DAYS",
                                      "conditionValue": 7
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "Badge created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "CreateBadgeSuccessResponse",
                                    summary = "Sample created badge response",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "id": 1,
                                                "code": "streak-7-days",
                                                "name": "7-Day Streak Master",
                                                "description": "Maintain a learning streak for 7 consecutive days",
                                                "iconUrl": "https://cdn.elingo.com/gamification/badges/streak-7-days.png",
                                                "conditionType": "STREAK_DAYS",
                                                "conditionValue": 7,
                                                "createdAt": "2026-11-21T10:00:00"
                                              }
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Invalid payload or duplicate badge code")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Authentication token is missing or expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    public ApiResponse<AdminBadgeResponse> createBadge(
            @Valid @RequestBody BadgeRequest request) {
        return ApiResponse.<AdminBadgeResponse>builder()
                .success(true)
                .data(adminBadgeService.createBadge(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update badge", description = "Updates an existing badge")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Badge update payload",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = BadgeRequest.class),
                    examples = @ExampleObject(
                            name = "UpdateBadgeRequestExample",
                            summary = "Sample badge update payload",
                            value = """
                                    {
                                      "code": "streak-7-days",
                                      "name": "7-Day Streak Master Updated",
                                      "description": "Maintain a learning streak for 7 consecutive days without missing a day",
                                      "iconUrl": "gamification/badges/streak-7-days-v2.png",
                                      "conditionType": "STREAK_DAYS",
                                      "conditionValue": 7
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "Badge updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "UpdateBadgeSuccessResponse",
                                    summary = "Sample updated badge response",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "id": 1,
                                                "code": "streak-7-days",
                                                "name": "7-Day Streak Master Updated",
                                                "description": "Maintain a learning streak for 7 consecutive days without missing a day",
                                                "iconUrl": "https://cdn.elingo.com/gamification/badges/streak-7-days-v2.png",
                                                "conditionType": "STREAK_DAYS",
                                                "conditionValue": 7,
                                                "createdAt": "2026-11-21T10:00:00"
                                              }
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Invalid payload or duplicate badge code")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Authentication token is missing or expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found - Badge does not exist")
    public ApiResponse<AdminBadgeResponse> updateBadge(
            @Parameter(description = "Unique ID of the badge to update", example = "1")
            @PathVariable @Positive(message = "INVALID_REQUEST") Long id,
            @Valid @RequestBody BadgeRequest request) {
        return ApiResponse.<AdminBadgeResponse>builder()
                .success(true)
                .data(adminBadgeService.updateBadge(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete badge", description = "Deletes a badge if it has not been earned by any user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
                    description = "Badge deleted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "DeleteBadgeSuccessResponse",
                                    summary = "Sample delete badge response",
                                    value = """
                                            {
                                              "success": true
                                            }
                                            """
                            )
                    )
            )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Badge is in use and cannot be deleted")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Authentication token is missing or expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin privileges required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found - Badge does not exist")
    public ApiResponse<Void> deleteBadge(
            @Parameter(description = "Unique ID of the badge to delete", example = "1")
            @PathVariable @Positive(message = "INVALID_REQUEST") Long id) {
        adminBadgeService.deleteBadge(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
