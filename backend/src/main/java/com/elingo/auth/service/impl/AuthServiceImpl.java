package com.elingo.auth.service.impl;

import com.elingo.auth.dto.request.AuthenticationRequest;
import com.elingo.auth.dto.request.RegisterRequest;
import com.elingo.auth.dto.request.ResetPasswordRequest;
import com.elingo.auth.dto.response.AuthenticationResponse;
import com.elingo.auth.dto.response.LoginResult;
import com.elingo.auth.service.AuthService;
import com.elingo.auth.service.JwtService;
import com.elingo.common.service.EmailService;
import com.elingo.common.service.RedisService;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.util.email.EmailTemplateName;
import com.elingo.user.dto.response.UserResponse;
import com.elingo.user.entity.User;
import com.elingo.user.mapper.UserMapper;
import com.elingo.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "AUTH-SERVICE")
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisService redisService;
    private final EmailService emailService;
    private final UserMapper userMapper;

    @Value("${jwt.refreshTokenTime:14}")
    private int refreshTokenDays;

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    private static final String OTP_PREFIX = "RESET_PW_OTP:";
    private static final long OTP_EXPIRATION_MINUTES = 15;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Processing user registration: username={}, email={}", request.username(), request.email());

        if (userRepository.existsByUsername(request.username()))
            throw new AppException(AppError.USERNAME_EXISTED);

        if (userRepository.existsByEmail(request.email()))
            throw new AppException(AppError.EMAIL_EXISTED);

        User user = userMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        userRepository.save(user);

        log.info("User registered successfully: userId={}, username={}", user.getId(), user.getUsername());
        return userMapper.toUserResponse(user);
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

        String userId = String.valueOf(user.getId());
        String accessToken = jwtService.generateAccessToken(userId, user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(userId);

        ResponseCookie refreshCookie = buildRefreshTokenCookie(refreshToken, Duration.ofDays(refreshTokenDays));

        log.info("User logged in successfully: userId={}, username={}", user.getId(), user.getUsername());
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

    private String generateOTP() {
        int length = 6;
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(10);// Tạo chỉ số từ 0 đến 9
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }

    @Override
    public void sendResetPasswordOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(AppError.EMAIL_NOT_EXISTED));

        String otp = generateOTP();
        redisService.save(OTP_PREFIX + email, otp, OTP_EXPIRATION_MINUTES);
        try {
            emailService.sendEmail(
                    user.getEmail(),
                    user.getFullName(),
                    EmailTemplateName.SEND_OTP,
                    otp,
                    "Reset Password"
            );
        } catch (MessagingException e) {
            log.error("Failed to send OTP for email {}: {}", email, e.getMessage());
            throw new AppException(AppError.EMAIL_SEND_FAILED);
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword()))
            throw new AppException(AppError.CONFIRM_PASSWORD_NOT_MATCH);

        String otp = redisService.get(OTP_PREFIX + request.email());
        if (otp == null || !otp.equals(request.otp()))
            throw new AppException(AppError.OTP_INVALID);

        String hashedPassword = passwordEncoder.encode(request.newPassword());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        user.setPasswordHash(hashedPassword);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        redisService.delete(OTP_PREFIX + request.email());
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
}
