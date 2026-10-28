package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.dto.request.CreateUserTopicRequest;
import com.elingo.vocabulary.dto.request.UpdateUserTopicRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
    @Operation(summary = "Get all cards belonging to a topic")
    public ApiResponse<List<CardResponse>> getCardsByTopic(
            @PathVariable Long topicId,
            @CurrentUserId Long currentUserId) {
        List<CardResponse> cards = topicService.getCardsByTopic(topicId, currentUserId);
        return ApiResponse.<List<CardResponse>>builder()
                .success(true)
                .data(cards)
                .build();
    }

    @PostMapping
    public ApiResponse<TopicResponse> createUserTopic(
            @Valid @RequestBody CreateUserTopicRequest request,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<TopicResponse>builder()
                .success(true)
                .data(topicService.createUserTopic(currentUserId, request))
                .build();
    }

    @PutMapping("/{topicId}")
    public ApiResponse<TopicResponse> updateUserTopic(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long topicId,
            @Valid @RequestBody UpdateUserTopicRequest request,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.<TopicResponse>builder()
                .success(true)
                .data(topicService.updateUserTopic(currentUserId, topicId, request))
                .build();
    }

    @DeleteMapping("/{topicId}")
    public ApiResponse<Void> deleteUserTopic(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long topicId,
            @CurrentUserId Long currentUserId) {
        topicService.deleteUserTopic(currentUserId, topicId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
