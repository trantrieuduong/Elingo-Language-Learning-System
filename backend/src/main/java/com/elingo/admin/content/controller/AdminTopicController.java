package com.elingo.admin.content.controller;

import com.elingo.admin.content.dto.request.CreateSystemTopicRequest;
import com.elingo.admin.content.dto.request.ReorderItemRequest;
import com.elingo.admin.content.dto.request.UpdateSystemTopicRequest;
import com.elingo.admin.content.service.AdminCardService;
import com.elingo.admin.content.service.AdminTopicService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/admin/topics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTopicController {

    private final AdminTopicService adminTopicService;
    private final AdminCardService adminCardService;

    @PostMapping
    public ApiResponse<TopicResponse> createSystemTopic(
            @Valid @RequestBody CreateSystemTopicRequest request) {
        return ApiResponse.<TopicResponse>builder()
                .success(true)
                .data(adminTopicService.createSystemTopic(request))
                .build();
    }

    @PutMapping("/{topicId}")
    public ApiResponse<TopicResponse> updateSystemTopic(
            @PathVariable Long topicId,
            @Valid @RequestBody UpdateSystemTopicRequest request) {
        return ApiResponse.<TopicResponse>builder()
                .success(true)
                .data(adminTopicService.updateSystemTopic(topicId, request))
                .build();
    }

    @DeleteMapping("/{topicId}")
    public ApiResponse<Void> deleteSystemTopic(@PathVariable Long topicId) {
        adminTopicService.deleteSystemTopic(topicId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @GetMapping("/{topicId}/cards")
    public ApiResponse<List<CardResponse>> getCardsByTopicId(@PathVariable Long topicId) {
        return ApiResponse.<List<CardResponse>>builder()
                .success(true)
                .data(adminCardService.getCardsByTopicId(topicId))
                .build();
    }

    @PatchMapping("/{topicId}/cards/orders")
    public ApiResponse<Void> reorderSystemCards(
            @PathVariable Long topicId,
            @Valid @RequestBody ReorderItemRequest request) {
        adminCardService.reorderSystemCards(topicId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
