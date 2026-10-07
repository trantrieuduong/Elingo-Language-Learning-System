package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.dto.response.CefrLevelResponse;
import com.elingo.vocabulary.service.CefrLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cefr-levels")
@RequiredArgsConstructor
@Tag(name = "CEFR Level Controller")
public class CefrLevelController {
    private final CefrLevelService cefrLevelService;

    @GetMapping
    @Operation(summary = "Get all CEFR levels")
    public ApiResponse<List<CefrLevelResponse>> getAllCefrLevels(@CurrentUserId Long currentUserId) {
        List<CefrLevelResponse> cefrLevels = cefrLevelService.getAllCefrLevels(currentUserId);
        return ApiResponse.<List<CefrLevelResponse>>builder()
                .success(true)
                .data(cefrLevels)
                .build();
    }
}
