package com.elingo.auth.controller;

import com.elingo.auth.dto.request.AuthenticationRequest;
import com.elingo.auth.dto.request.RegisterRequest;
import com.elingo.auth.dto.request.ResetPasswordRequest;
import com.elingo.auth.dto.request.SendResetPasswordOtpRequest;
import com.elingo.auth.dto.response.AuthenticationResponse;
import com.elingo.auth.dto.response.LoginResult;
import com.elingo.auth.service.AuthService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Slf4j(topic = "AUTHENTICATION-CONTROLLER")
@RequiredArgsConstructor
@Tag(name = "Authentication Controller")
public class AuthenticationController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Sign up new user account")
    public ApiResponse<UserResponse> signUp(@RequestBody @Valid RegisterRequest registerRequest) {
        log.info("User sign up request: username={}", registerRequest.username());
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .data(authService.register(registerRequest))
                .build();
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with username/email and password")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @RequestBody @Valid AuthenticationRequest authenticationRequest) {
        log.info("User login request: identifier={}", authenticationRequest.username());
        LoginResult loginResult = authService.login(authenticationRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, loginResult.refreshCookie().toString())
                .body(ApiResponse.<AuthenticationResponse>builder()
                        .success(true)
                        .data(loginResult.response())
                        .build());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using HTTP-only refresh token cookie")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        log.info("Refresh token request received");
        LoginResult loginResult = authService.refreshToken(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, loginResult.refreshCookie().toString())
                .body(ApiResponse.<AuthenticationResponse>builder()
                        .success(true)
                        .data(loginResult.response())
                        .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out user and clear refresh token cookie")
    public ResponseEntity<ApiResponse<Void>> logout() {
        log.info("User logout request received");
        ResponseCookie clearCookie = authService.createLogoutCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .build());
    }

    @PostMapping("/password-reset/otp")
    @Operation(summary = "Send OTP for reset password via email")
    public ApiResponse<Void> sendResetPasswordOtp(
            @Valid @RequestBody SendResetPasswordOtpRequest request) {
        log.info("Send reset password OTP request received: email={}", request.email());
        authService.sendResetPasswordOtp(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping("/password-reset")
    @Operation(summary = "Reset password with OTP")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset Password request received: email={}", request.email());
        authService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
