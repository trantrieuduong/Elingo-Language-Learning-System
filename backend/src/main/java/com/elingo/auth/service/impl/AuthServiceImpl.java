package com.elingo.auth.service.impl;

import com.elingo.auth.dto.request.AuthenticationRequest;
import com.elingo.auth.dto.request.GoogleAuthRequest;
import com.elingo.auth.dto.request.RegisterRequest;
import com.elingo.auth.dto.request.ResendVerificationOtpRequest;
import com.elingo.auth.dto.request.ResetPasswordRequest;
import com.elingo.auth.dto.request.SendResetPasswordOtpRequest;
import com.elingo.auth.dto.request.VerifyAccountRequest;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import com.elingo.auth.dto.response.AuthenticationResponse;
import com.elingo.auth.dto.response.LoginResult;
import com.elingo.auth.service.AuthService;
import com.elingo.auth.service.GoogleTokenVerifierService;
import com.elingo.auth.service.JwtService;
import com.elingo.common.service.EmailService;
import com.elingo.common.service.OtpService;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.util.EmailTemplateName;
import com.elingo.common.util.OtpType;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.entity.User;
import com.elingo.user.mapper.UserMapper;
import com.elingo.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "AUTH-SERVICE")
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final OtpService otpService;
    private final UserMapper userMapper;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    @Value("${jwt.refresh-token-time:14}")
    private int refreshTokenDays;

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Override
    @Transactional
    public UserMeResponse register(RegisterRequest request) {
        log.info("Processing user registration: username={}, email={}", request.username(), request.email());

        if (userRepository.existsByUsername(request.username()))
            throw new AppException(AppError.USERNAME_EXISTED);

        if (userRepository.existsByEmail(request.email()))
            throw new AppException(AppError.EMAIL_EXISTED);

        User user = userMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setIsVerified(false);

        userRepository.save(user);

        String otp = otpService.generateAndSaveOtp(OtpType.VERIFY_ACCOUNT, user.getEmail());
        emailService.sendEmail(
                user.getEmail(),
                user.getUsername(),
                EmailTemplateName.SEND_OTP,
                otp,
                OtpType.VERIFY_ACCOUNT.getTitle()
        );

        log.info("User registered successfully and verification OTP sent: userId={}, username={}", user.getId(),
                user.getUsername());
        return userMapper.toUserMeResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult login(AuthenticationRequest request) {
        log.info("Processing login for identifier: {}", request.username());

        User user = userRepository.findByUsernameOrEmail(request.username(), request.username())
                .orElseThrow(() -> new AppException(AppError.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw new AppException(AppError.INVALID_CREDENTIALS);

        if (Boolean.FALSE.equals(user.getIsActive()))
            throw new AppException(AppError.USER_INACTIVE);

        if (Boolean.FALSE.equals(user.getIsVerified())) {
            log.info("User account not verified yet, sending new verification OTP: userId={}", user.getId());
            String otp = otpService.generateAndSaveOtp(OtpType.VERIFY_ACCOUNT, user.getEmail());
            emailService.sendEmail(
                    user.getEmail(),
                    user.getUsername(),
                    EmailTemplateName.SEND_OTP,
                    otp,
                    OtpType.VERIFY_ACCOUNT.getTitle()
            );
            throw new AppException(AppError.USER_NOT_VERIFIED);
        }

        String userId = String.valueOf(user.getId());
        String accessToken = jwtService.generateAccessToken(userId, user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(userId);

        ResponseCookie refreshCookie = buildRefreshTokenCookie(refreshToken, Duration.ofDays(refreshTokenDays));

        log.info("User logged in successfully: userId={}, username={}", user.getId(), user.getUsername());
        return new LoginResult(new AuthenticationResponse(accessToken), refreshCookie);
    }

    @Override
    @Transactional
    public LoginResult authenticateWithGoogle(GoogleAuthRequest request) {
        log.info("Processing Google authentication, verifying token...");

        GoogleIdToken.Payload payload = googleTokenVerifierService.verify(request.idToken());
        log.info("Google token verified successfully for email: {}", payload.getEmail());

        User user = userRepository.findByGoogleProviderId(payload.getSubject())
                .orElseGet(() -> linkOrCreateByEmail(payload));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AppException(AppError.USER_INACTIVE);
        }

        String userId = String.valueOf(user.getId());
        String accessToken = jwtService.generateAccessToken(userId, user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(userId);
        ResponseCookie refreshCookie = buildRefreshTokenCookie(refreshToken, Duration.ofDays(refreshTokenDays));

        log.info("Google authentication successful: userId={}, email={}", user.getId(), user.getEmail());
        return new LoginResult(new AuthenticationResponse(accessToken), refreshCookie);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult refreshToken(String refreshToken) {
        log.info("Processing refresh token request");

        if (!StringUtils.hasText(refreshToken))
            throw new AppException(AppError.REFRESH_TOKEN_INVALID);

        Claims claims;
        try {
            claims = jwtService.getClaimsJws(refreshToken).getBody();
        } catch (JwtException ex) {
            log.warn("Invalid refresh token: {}", ex.getMessage());
            throw new AppException(AppError.REFRESH_TOKEN_INVALID);
        }

        String tokenType = claims.get("token_type", String.class);
        if (!"REFRESH".equals(tokenType)) {
            log.warn("Token is not a refresh token: tokenType={}", tokenType);
            throw new AppException(AppError.REFRESH_TOKEN_INVALID);
        }

        Long userId = Long.parseLong(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        if (Boolean.FALSE.equals(user.getIsActive()))
            throw new AppException(AppError.USER_INACTIVE);

        String newAccessToken = jwtService.generateAccessToken(String.valueOf(user.getId()), user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(String.valueOf(user.getId()));

        ResponseCookie refreshCookie = buildRefreshTokenCookie(newRefreshToken, Duration.ofDays(refreshTokenDays));

        log.info("Token refreshed successfully for userId={}", user.getId());
        return new LoginResult(new AuthenticationResponse(newAccessToken), refreshCookie);
    }

    @Override
    public ResponseCookie createLogoutCookie() {
        return buildRefreshTokenCookie("", Duration.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public void sendResetPasswordOtp(SendResetPasswordOtpRequest request) {
        log.info("Processing send reset password OTP for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(AppError.EMAIL_NOT_EXISTED));
        String otp = otpService.generateAndSaveOtp(OtpType.RESET_PASSWORD, request.email());
        emailService.sendEmail(
                user.getEmail(),
                user.getUsername(),
                EmailTemplateName.SEND_OTP,
                otp,
                OtpType.RESET_PASSWORD.getTitle()
        );
        log.info("Reset password OTP dispatched successfully to email: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing reset password for email: {}", request.email());

        otpService.verifyOtp(OtpType.RESET_PASSWORD, request.email(), request.otp());

        String hashedPassword = passwordEncoder.encode(request.newPassword());
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(AppError.EMAIL_NOT_EXISTED));
        user.setPasswordHash(hashedPassword);
        user.setPasswordChangedAt(LocalDateTime.now());
        log.info("Password reset successfully for user: userId={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    @Transactional
    public void verifyAccount(VerifyAccountRequest request) {
        log.info("Processing account verification for email: {}", request.email());

        otpService.verifyOtp(OtpType.VERIFY_ACCOUNT, request.email(), request.otp());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(AppError.EMAIL_NOT_EXISTED));

        user.setIsVerified(true);
        log.info("Account verified successfully: userId={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public void resendVerificationOtp(ResendVerificationOtpRequest request) {
        log.info("Processing resend verification OTP for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(AppError.EMAIL_NOT_EXISTED));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            log.info("User account is already verified: userId={}", user.getId());
            return;
        }

        String otp = otpService.generateAndSaveOtp(OtpType.VERIFY_ACCOUNT, request.email());
        emailService.sendEmail(
                user.getEmail(),
                user.getUsername(),
                EmailTemplateName.SEND_OTP,
                otp,
                OtpType.VERIFY_ACCOUNT.getTitle()
        );
        log.info("Verification OTP resent successfully to email: {}", user.getEmail());
    }

    private ResponseCookie buildRefreshTokenCookie(String value, Duration maxAge) {
        String cookiePath = contextPath.endsWith("/") ? contextPath + "auth" : contextPath + "/auth";
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(false)
                .path(cookiePath)
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
    }

    private User linkOrCreateByEmail(GoogleIdToken.Payload payload) {
        return userRepository.findByEmail(payload.getEmail())
                .map(existing -> applyGoogleLink(existing, payload))
                .orElseGet(() -> createFromGoogle(payload));
    }

    private User applyGoogleLink(User existing, GoogleIdToken.Payload payload) {
        if (!Boolean.TRUE.equals(existing.getIsVerified()))
            existing.setPasswordHash(null);

        existing.setGoogleProviderId(payload.getSubject());
        existing.setIsVerified(true);

        log.info("Google account linked to existing user: userId={}, email={}", existing.getId(), existing.getEmail());
        return userRepository.save(existing);
    }

    private User createFromGoogle(GoogleIdToken.Payload payload) {
        String fullName = (String) payload.get("name");
        User user = User.builder()
                .email(payload.getEmail())
                .username("u" + UUID.randomUUID().toString().replace("-", "").substring(0, 13))
                .fullName(fullName != null ? fullName : payload.getEmail().split("@")[0])
                .googleProviderId(payload.getSubject())
                .isVerified(true)
                .build();
        return userRepository.save(user);
    }
}
