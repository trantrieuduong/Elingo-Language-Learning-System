package com.elingo.user.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.common.service.EmailService;
import com.elingo.common.service.OtpService;
import com.elingo.common.util.EmailTemplateName;
import com.elingo.common.util.OtpType;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements UserService {
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("Processing change password request for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            log.warn("Change password failed: Incorrect old password for userId={}", userId);
            throw new AppException(AppError.OLD_PASSWORD_INCORRECT);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            log.warn("Change password failed: New password is same as old password for userId={}", userId);
            throw new AppException(AppError.NEW_PASSWORD_SAME_AS_OLD);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());

        log.info("Password changed successfully for userId={}", user.getId());
    }

    @Override
    public void sendOTPUpdateEmail(Long userId, SendOTPUpdateEmailRequest request) {
        String newEmail = request.newEmail();
        log.info("Processing send otp update email request: email={}, userId={}", newEmail, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        String oldEmail = user.getEmail();
        if (oldEmail.equals(newEmail)) {
            log.info("Send OTP update email failed: Email is unchange for email={}, userId={}", newEmail, userId);
            throw new AppException(AppError.EMAIL_UNCHANGED);
        }

        if (userRepository.existsByEmail(newEmail)) {
            log.info("Send OTP update email failed: Email is existed for email={}, userId={}", newEmail, userId);
            throw new AppException(AppError.EMAIL_EXISTED);
        }

        String newEmailOtp = otpService.generateAndSaveOtp(OtpType.CHANGE_EMAIL, newEmail);
        emailService.sendEmail(
                newEmail,
                user.getUsername(),
                EmailTemplateName.SEND_OTP,
                newEmailOtp,
                "Verify Email For Email Update"
        );
        log.info("Update email OTP sent successfully: email={}, userId={}", newEmail, userId);
    }

    @Override
    public void updateEmail(Long userId, UpdateEmailRequest request) {
        String newEmail = request.newEmail();
        log.info("Processing update email: email={}, userId={}", newEmail, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        if (userRepository.existsByEmail(newEmail)) {
            log.warn("Update email failed: Email is existed for email={}, userId={}", newEmail, userId);
            throw new AppException(AppError.EMAIL_EXISTED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Update email failed: Incorrect password for userId={}", userId);
            throw new AppException(AppError.PASSWORD_INCORRECT);
        }

        otpService.verifyOtp(OtpType.CHANGE_EMAIL, newEmail, request.newEmailOtp());

        user.setEmail(newEmail);
        userRepository.save(user);
        log.info("Email updated successfully for email={}, userId={}", newEmail, userId);
    }
}
