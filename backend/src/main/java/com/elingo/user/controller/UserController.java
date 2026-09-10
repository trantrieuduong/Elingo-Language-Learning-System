package com.elingo.user.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.user.dto.request.ChangePasswordRequest;
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
}