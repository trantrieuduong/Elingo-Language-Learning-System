package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.dto.response.TagResponse;
import com.elingo.vocabulary.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
@Tag(name = "Tag Controller")
public class TagController {
    private final TagService tagService;

    @GetMapping
    @Operation(summary = "Get all tags")
    public ApiResponse<List<TagResponse>> getAllTags(@CurrentUserId Long currentUserId) {
        List<TagResponse> tags = tagService.getAllTags(currentUserId);
        return ApiResponse.<List<TagResponse>>builder()
                .success(true)
                .data(tags)
                .build();
    }
}
