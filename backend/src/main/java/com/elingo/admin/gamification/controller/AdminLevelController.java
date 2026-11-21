package com.elingo.admin.gamification.controller;

import com.elingo.admin.gamification.dto.request.LevelRequest;
import com.elingo.admin.gamification.dto.response.AdminLevelResponse;
import com.elingo.admin.gamification.service.AdminLevelService;
import com.elingo.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/gamification/levels")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin Level Controller",
        description = "Management endpoints for level XP thresholds in the admin console"
)
public class AdminLevelController {

    private final AdminLevelService adminLevelService;

    @GetMapping
    @Operation(summary = "Get all levels", description = "Retrieves all level thresholds sorted by level number")
    public ApiResponse<List<AdminLevelResponse>> getAllLevels() {
        return ApiResponse.<List<AdminLevelResponse>>builder()
                .success(true)
                .data(adminLevelService.getAllLevels())
                .build();
    }

    @GetMapping("/{levelNo}")
    @Operation(summary = "Get level by number", description = "Retrieves a specific level threshold by level number")
    public ApiResponse<AdminLevelResponse> getLevelByNo(
            @PathVariable @Positive(message = "INVALID_REQUEST") Short levelNo) {
        return ApiResponse.<AdminLevelResponse>builder()
                .success(true)
                .data(adminLevelService.getLevelByNo(levelNo))
                .build();
    }

    @PostMapping
    @Operation(summary = "Create a new level", description = "Creates a new level threshold (level number must be > 1)")
    public ApiResponse<AdminLevelResponse> createLevel(
            @Valid @RequestBody LevelRequest request) {
        return ApiResponse.<AdminLevelResponse>builder()
                .success(true)
                .data(adminLevelService.createLevel(request))
                .build();
    }

    @PutMapping("/{levelNo}")
    @Operation(summary = "Update level", description = "Updates a level threshold. Default level 1 required XP cannot be changed")
    public ApiResponse<AdminLevelResponse> updateLevel(
            @PathVariable @Positive(message = "INVALID_REQUEST") Short levelNo,
            @Valid @RequestBody LevelRequest request) {
        return ApiResponse.<AdminLevelResponse>builder()
                .success(true)
                .data(adminLevelService.updateLevel(levelNo, request))
                .build();
    }

    @DeleteMapping("/{levelNo}")
    @Operation(summary = "Delete level", description = "Deletes a level threshold. Default level 1 cannot be deleted")
    public ApiResponse<Void> deleteLevel(
            @PathVariable @Positive(message = "INVALID_REQUEST") Short levelNo) {
        adminLevelService.deleteLevel(levelNo);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
