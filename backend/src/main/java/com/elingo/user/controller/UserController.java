package com.elingo.user.controller;

import com.elingo.common.dto.ApiResponse;
import com.elingo.common.util.SecurityUtils;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Slf4j(topic = "USER-CONTROLLER")
@RequiredArgsConstructor
@Tag(name = "User Controller")
public class UserController {
    private final UserService userService;

    @PostMapping("/email/send-otp")
    @Operation(summary = "Request to update email (Sends OTP)")
    public ResponseEntity<ApiResponse<Void>> SendOTPUpdateEmail(
            @Valid @RequestBody SendOTPUpdateEmailRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Send OTP update email request received: username={}", username);
        userService.sendOTPUpdateEmail(username, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .build());
    }

    @PatchMapping("/email")
    @Operation(summary = "Confirm email update with OTP")
    public ResponseEntity<ApiResponse<Void>> updateEmail(
            @Valid @RequestBody UpdateEmailRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Update email request received: username={}", username);
        userService.updateEmail(username, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .build());
    }
}
