package com.elingo.premium.service.impl;

import com.elingo.premium.entity.PaymentProvider;
import com.elingo.premium.entity.PaymentWebhookLog;
import com.elingo.premium.entity.WebhookLogStatus;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.repository.PaymentWebhookLogRepository;
import com.elingo.premium.service.WebhookLogWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "WEBHOOK-LOG-WRITER")
public class WebhookLogWriterImpl implements WebhookLogWriter {

    private final PaymentWebhookLogRepository webhookLogRepository;
    private final PaymentTransactionRepository transactionRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<PaymentWebhookLog> saveReceived(
            PaymentProvider provider,
            String eventId,
            String rawPayload,
            Boolean signatureValid
    ) {
        String effectiveEventId = eventId;
        if (effectiveEventId == null || effectiveEventId.isBlank()) {
            effectiveEventId = calculateSha256(rawPayload);
        }

        if (provider != null && provider.getId() != null) {
            boolean exists = webhookLogRepository.existsByProviderIdAndEventId(provider.getId(), effectiveEventId);
            if (exists) {
                log.info("Duplicate webhook event received providerId={} eventId={}",
                        provider.getId(), effectiveEventId);
                return Optional.empty();
            }
        }

        PaymentWebhookLog logEntry = PaymentWebhookLog.builder()
                .provider(provider)
                .eventId(effectiveEventId)
                .payload(rawPayload)
                .signatureValid(signatureValid)
                .status(WebhookLogStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .build();

        try {
            PaymentWebhookLog saved = webhookLogRepository.saveAndFlush(logEntry);
            log.info("Saved webhook log id={} providerId={} eventId={}",
                    saved.getId(), provider != null ? provider.getId() : null, effectiveEventId);
            return Optional.of(saved);
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate webhook event caught by constraint providerId={} eventId={}",
                    provider != null ? provider.getId() : null, effectiveEventId);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDone(Long logId, Long transactionId, WebhookLogStatus status) {
        if (logId == null) {
            return;
        }

        webhookLogRepository.findById(logId).ifPresent(entry -> {
            if (transactionId != null) {
                transactionRepository.findById(transactionId).ifPresent(entry::setTransaction);
            }
            entry.setStatus(status);
            entry.setProcessedAt(LocalDateTime.now());
            webhookLogRepository.save(entry);
            log.info("Updated webhook log id={} txId={} status={}", logId, transactionId, status);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long logId, Long transactionId) {
        if (logId == null) {
            return;
        }

        webhookLogRepository.findById(logId).ifPresent(entry -> {
            if (transactionId != null) {
                transactionRepository.findById(transactionId).ifPresent(entry::setTransaction);
            }
            entry.setStatus(WebhookLogStatus.FAILED);
            entry.setProcessedAt(LocalDateTime.now());
            webhookLogRepository.save(entry);
            log.warn("Marked webhook log failed id={} txId={}", logId, transactionId);
        });
    }

    private String calculateSha256(String data) {
        if (data == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

