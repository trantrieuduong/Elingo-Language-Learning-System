package com.elingo.common.service.impl;

import com.elingo.common.enums.EmailTemplateName;
import com.elingo.common.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-SERVICE")
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    @Value("${app.otp.expiration-minutes:15}")
    private long otpExpirationMinutes;

    @Async("emailTaskExecutor")
    @Override
    public void sendEmail(
            String toEmail,
            String username,
            EmailTemplateName emailTemplateName,
            String otp,
            String subject
    ) {
        String templateName = Objects.requireNonNullElse(emailTemplateName, EmailTemplateName.SEND_OTP)
                .getName();

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED,
                    StandardCharsets.UTF_8.name()
            );
            Map<String, Object> properties = new HashMap<>();
            properties.put("username", username);
            properties.put("otp", otp);
            properties.put("subject", subject);
            properties.put("expirationMinutes", otpExpirationMinutes);

            Context context = new Context();
            context.setVariables(properties);
            mimeMessageHelper.setTo(toEmail);
            mimeMessageHelper.setSubject(subject);

            String template = templateEngine.process(templateName, context);
            mimeMessageHelper.setText(template, true);

            javaMailSender.send(mimeMessage);
            log.info("Email sent to={} template={}", toEmail, templateName);
        } catch (MessagingException | MailException e) {
            log.warn("Email sending failed to={} template={} username={}", toEmail, templateName, username, e);
        }
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendPaymentSuccessEmail(
            String toEmail,
            String username,
            String planName,
            BigDecimal amount,
            String currency,
            String refCode,
            LocalDateTime paidAt,
            LocalDateTime endAt
    ) {
        String templateName = EmailTemplateName.PAYMENT_SUCCESS.getName();
        String subject = "Premium Subscription Activated Successfully";

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED,
                    StandardCharsets.UTF_8.name()
            );

            NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.GERMANY);
            String formattedAmount = (amount != null) ? currencyFormat.format(amount) : "0";
            String formattedPaidAt = (paidAt != null) ? paidAt.format(DATE_TIME_FORMATTER) : "";
            String formattedEndAt = (endAt != null) ? endAt.format(DATE_TIME_FORMATTER) : "";

            Map<String, Object> properties = new HashMap<>();
            properties.put("username", username != null ? username : "");
            properties.put("subject", subject);
            properties.put("refCode", refCode != null ? refCode : "");
            properties.put("planName", planName != null ? planName : "");
            properties.put("amount", formattedAmount);
            properties.put("currency", currency != null ? currency : "VND");
            properties.put("paidAt", formattedPaidAt);
            properties.put("endAt", formattedEndAt);

            Context context = new Context();
            context.setVariables(properties);
            mimeMessageHelper.setTo(toEmail);
            mimeMessageHelper.setSubject(subject);

            String template = templateEngine.process(templateName, context);
            mimeMessageHelper.setText(template, true);

            javaMailSender.send(mimeMessage);
            log.info("Payment success email sent to={} refCode={}", toEmail, refCode);
        } catch (MessagingException | MailException e) {
            log.warn("Failed to send payment success email to={} refCode={}", toEmail, refCode, e);
        }
    }
}
