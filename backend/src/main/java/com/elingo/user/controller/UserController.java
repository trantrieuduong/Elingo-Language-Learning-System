package com.elingo.user.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.SetPasswordRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Slf4j(topic = "USER-CONTROLLER")
@RequiredArgsConstructor
@Tag(name = "User Controller")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ApiResponse<UserMeResponse> getMyInfo(@CurrentUserId Long userId) {
        log.info("Get current user profile request received: userId={}", userId);
        UserMeResponse response = userService.getMyInfo(userId);
        return ApiResponse.<UserMeResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile by id")
    public ApiResponse<?> getUserById(
            @PathVariable Long id,
            @CurrentUserId Long currentUserId
    ) {
        log.info("Get user profile by id request received: targetId={}, currentUserId={}", id, currentUserId);
        Object response = userService.getUserById(id, currentUserId);
        return ApiResponse.builder()
                .success(true)
                .data(response)
                .build();
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Change current user password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @CurrentUserId Long userId
    ) {
        log.info("Change password request received: userId={}", userId);
        userService.changePassword(userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping("/me/password")
    @Operation(summary = "Set password for Google-authenticated account (first time only)")
    public ApiResponse<Void> setPassword(
            @Valid @RequestBody SetPasswordRequest request,
            @CurrentUserId Long userId
    ) {
        log.info("Set password request received: userId={}", userId);
        userService.setPassword(userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping("/me/email/otp")
    @Operation(summary = "Send OTP for update email via email")
    public ApiResponse<Void> sendOTPUpdateEmail(
            @Valid @RequestBody SendOTPUpdateEmailRequest request,
            @CurrentUserId Long userId) {
        log.info("Send OTP update email request received: userId={}", userId);
        userService.sendOTPUpdateEmail(userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PatchMapping("/me/email")
    @Operation(summary = "Confirm email update with OTP")
    public ApiResponse<Void> updateEmail(
            @Valid @RequestBody UpdateEmailRequest request,
            @CurrentUserId Long userId) {
        log.info("Update email request received: userId={}", userId);
        userService.updateEmail(userId, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
