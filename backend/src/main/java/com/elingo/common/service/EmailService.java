package com.elingo.common.service;

import com.elingo.common.enums.EmailTemplateName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EmailService {
    void sendEmail(
            String toEmail,
            String username,
            EmailTemplateName emailTemplateName,
            String otp,
            String subject
    );

    void sendPaymentSuccessEmail(
            String toEmail,
            String username,
            String planName,
            BigDecimal amount,
            String currency,
            String refCode,
            LocalDateTime paidAt,
            LocalDateTime endAt
    );
}
