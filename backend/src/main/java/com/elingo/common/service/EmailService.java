package com.elingo.common.service;

import com.elingo.common.util.EmailTemplateName;

public interface EmailService {
    void sendEmail(
            String toEmail,
            String username,
            EmailTemplateName emailTemplateName,
            String otp,
            String subject
    );
}
