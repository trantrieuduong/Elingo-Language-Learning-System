package com.elingo.common.service.impl;

import com.elingo.common.service.EmailService;
import com.elingo.common.util.EmailTemplateName;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-SERVICE")
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String emailFrom;

    @Async("emailTaskExecutor")
    public void sendEmail(
            String toEmail,
            String username,
            EmailTemplateName emailTemplateName,
            String otp,
            String subject
    ) {

        String templateName;
        if (emailTemplateName == null)
            templateName = "send-otp";
        else
            templateName = emailTemplateName.getName();

        log.info("Sending email to={}, username={}, template={}, subject={}", toEmail, username, templateName, subject);

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED, // Chứa toàn bộ text, inline và tệp đính kèm chung một chỗ
                    StandardCharsets.UTF_8.name()
            );
            Map<String, Object> properties = new HashMap<>();
            properties.put("username", username);
            properties.put("otp", otp);

            Context context = new Context();
            context.setVariables(properties);
            mimeMessageHelper.setFrom(emailFrom);
            mimeMessageHelper.setTo(toEmail);
            mimeMessageHelper.setSubject(subject);

            String template = templateEngine.process(templateName, context);
            mimeMessageHelper.setText(template, true);

            javaMailSender.send(mimeMessage);
            log.info("Email sent successfully to {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Error sending email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
