package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.dto.request.CreateTopicRequest;
import com.elingo.vocabulary.dto.request.UpdateTopicRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.elingo.common.annotation.IsAdmin;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
@Tag(name = "Topic Controller")
public class TopicController {
    private final TopicService topicService;

    @GetMapping("/{topicId}/cards")
    @Operation(summary = "Get all unlearned cards belonging to a topic of current user")
    public ApiResponse<List<CardResponse>> getUnlearnedCardsByTopic(
            @PathVariable Long topicId,
            @CurrentUserId Long currentUserId) {
        List<CardResponse> cards = topicService.getUnlearnedCardsByTopic(currentUserId, topicId);
        return ApiResponse.<List<CardResponse>>builder()
                .success(true)
                .data(cards)
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a new user topic (User only)")
    public ApiResponse<TopicResponse> createUserTopic(
            @RequestBody @Valid CreateTopicRequest request,
            @CurrentUserId Long currentUserId) {
        TopicResponse topic = topicService.createUserTopic(request, currentUserId);
        return ApiResponse.<TopicResponse>builder()
                .success(true)
                .data(topic)
                .build();
    }

    @PostMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new system topic (Admin only)")
    public ApiResponse<TopicResponse> createSystemTopic(
            @RequestBody @Valid CreateTopicRequest request) {
        TopicResponse topic = topicService.createSystemTopic(request);
        return ApiResponse.<TopicResponse>builder()
                .success(true)
                .data(topic)
                .build();
    }

    @PutMapping("/{topicId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update a user topic (User only)")
    public ApiResponse<TopicResponse> updateUserTopic(
            @PathVariable Long topicId,
            @RequestBody @Valid UpdateTopicRequest request,
            @CurrentUserId Long currentUserId) {
        TopicResponse topic = topicService.updateUserTopic(topicId, request, currentUserId);
        return ApiResponse.<TopicResponse>builder()
                .success(true)
                .data(topic)
                .build();
    }

    @PutMapping("/{topicId}/system")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a system topic (Admin only)")
    public ApiResponse<TopicResponse> updateSystemTopic(
            @PathVariable Long topicId,
            @RequestBody @Valid UpdateTopicRequest request) {
        TopicResponse topic = topicService.updateSystemTopic(topicId, request);
        return ApiResponse.<TopicResponse>builder()
                .success(true)
                .data(topic)
                .build();
    }

    @DeleteMapping("/{topicId}")
    @Operation(summary = "Delete a topic (Soft delete if published, Hard delete if draft)")
    public ApiResponse<Void> deleteTopic(
            @PathVariable Long topicId,
            @CurrentUserId Long currentUserId,
            @IsAdmin boolean isAdmin) {
        topicService.deleteTopic(topicId, currentUserId, isAdmin);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
