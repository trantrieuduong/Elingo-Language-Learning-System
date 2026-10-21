package com.elingo.common.controller;

import com.elingo.common.dto.ApiResponse;
import com.elingo.common.service.SlugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/slugs")
@RequiredArgsConstructor
@Tag(name = "Slug Controller")
public class SlugController {
    private final SlugService slugService;

    @GetMapping
    @Operation(summary = "Generate a unique slug from title")
    public ApiResponse<String> generateSlug(@RequestParam String title) {
        String slug = slugService.generateUniqueSlug(title);
        return ApiResponse.<String>builder()
                .success(true)
                .data(slug)
                .build();
    }
}