package com.elingo.community.controller;

import com.elingo.common.dto.ApiResponse;
import com.elingo.community.dto.response.PostGroupResponse;
import com.elingo.community.service.PostGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/post-groups")
@RequiredArgsConstructor
@Tag(name = "Post Group Controller")
public class PostGroupController {

    private final PostGroupService postGroupService;

    @GetMapping
    @Operation(summary = "Get all post group")
    public ApiResponse<List<PostGroupResponse>> getPostGroups() {
        return ApiResponse.<List<PostGroupResponse>>builder()
                .success(true)
                .data(postGroupService.getPostGroups())
                .build();
    }
}
