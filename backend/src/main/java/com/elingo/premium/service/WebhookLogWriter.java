package com.elingo.premium.service;

import com.elingo.premium.entity.PaymentProvider;
import com.elingo.premium.entity.PaymentWebhookLog;
import com.elingo.premium.entity.WebhookLogStatus;

import java.util.Optional;

public interface WebhookLogWriter {

    Optional<PaymentWebhookLog> saveReceived(
            PaymentProvider provider,
            String eventId,
            String rawPayload,
            Boolean signatureValid
    );

    void markDone(Long logId, Long transactionId, WebhookLogStatus status);

    void markFailed(Long logId, Long transactionId);
}

