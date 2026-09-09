package com.elingo.user.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.service.EmailService;
import com.elingo.common.service.RedisService;
import com.elingo.common.util.EmailTemplateName;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.user.service.UserService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RedisService redisService;
    private final EmailService emailService;

    @Value("${app.old-email-otp-prefix}")
    private String oldEmailOtpPrefix;

    @Value("${app.new-email-otp-prefix}")
    private String newEmailOtpPrefix;

    @Value("${app.otp-expiration-minutes}")
    private long otpExpirationMinutes;

    private String generateOTP() {
        int length = 6;
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(10);
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }

    @Override
    public void sendOTPUpdateEmail(String username, SendOTPUpdateEmailRequest request) {
        log.info("Processing send otp update email request for user: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        String oldEmail = user.getEmail();
        String newEmail = request.email();
        if (oldEmail.equals(newEmail)) {
            throw new AppException(AppError.EMAIL_UNCHANGED);
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new AppException(AppError.EMAIL_EXISTED);
        }

        String oldEmailOtp = generateOTP();
        String newEmailOtp = generateOTP();

        redisService.save(oldEmailOtpPrefix + ":" + username, oldEmailOtp, otpExpirationMinutes);
        redisService.save(newEmailOtpPrefix + ":" + username, newEmailOtp, otpExpirationMinutes);


        try {
            emailService.sendEmail(
                    oldEmail,
                    username,
                    EmailTemplateName.SEND_OTP,
                    oldEmailOtp,
                    "Verify Old Email"
            );
            emailService.sendEmail(
                    newEmail,
                    username,
                    EmailTemplateName.SEND_OTP,
                    newEmailOtp,
                    "Verify New Email"
            );
            log.info("Sent update email OTP to {} (old), {} (new)", oldEmail, newEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP for updating email {} (old), {} (new): {}", oldEmail, newEmail, e.getMessage());
            throw new AppException(AppError.EMAIL_SEND_FAILED);
        }
    }

    @Override
    @Transactional
    public void updateEmail(String username, UpdateEmailRequest request) {
        log.info("Processing update email for user: username={}", username);
        
        String newEmailOtp = redisService.get(newEmailOtpPrefix + ":" + username);
        String oldEmailOtp = redisService.get(oldEmailOtpPrefix + ":" + username);
        if (newEmailOtp == null
                || oldEmailOtp == null
                || !newEmailOtp.equals(request.newEmailOtp())
                || !oldEmailOtp.equals(request.oldEmailOtp()))
            throw new AppException(AppError.OTP_INVALID);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        String newEmail = request.email();
        if (userRepository.existsByEmail(newEmail))
            throw new AppException(AppError.EMAIL_EXISTED);

        user.setEmail(newEmail);
        userRepository.save(user);

        redisService.delete(newEmailOtpPrefix + ":" + username);
        redisService.delete(oldEmailOtpPrefix + ":" + username);
        log.info("Successfully updated email for user: username={}", username);
    }
}
