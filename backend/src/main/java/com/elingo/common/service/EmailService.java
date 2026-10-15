package com.elingo.common.service;

import com.elingo.common.enums.EmailTemplateName;

public interface EmailService {
    void sendEmail(
            String toEmail,
            String username,
            EmailTemplateName emailTemplateName,
            String otp,
            String subject
    );
}
