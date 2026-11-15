package com.elingo.notification.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.notification.dto.response.UnreadCountResponse;
import com.elingo.notification.dto.response.NotificationResponse;
import com.elingo.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Controller")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get list notifications of user")
    public ApiResponse<PageResponse<NotificationResponse>> getAll(
            @CurrentUserId Long currentUserId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<NotificationResponse>>builder()
                .success(true)
                .data(notificationService.getAll(currentUserId, pageable))
                .build();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread notifications of user")
    public ApiResponse<UnreadCountResponse> countUnread(@CurrentUserId Long currentUserId) {
        return ApiResponse.<UnreadCountResponse>builder()
                .success(true)
                .data(notificationService.countUnread(currentUserId))
                .build();
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ApiResponse<Void> markRead(
            @CurrentUserId Long userId,
            @PathVariable @Positive(message = "INVALID_REQUEST") Long id) {
        notificationService.markRead(userId, id);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ApiResponse<Void> markAllRead(@CurrentUserId Long userId) {
        notificationService.markAllRead(userId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
