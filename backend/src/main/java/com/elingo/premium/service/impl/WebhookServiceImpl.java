package com.elingo.premium.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.dto.PaymentProcessingResult;
import com.elingo.premium.entity.PaymentProvider;
import com.elingo.premium.entity.PaymentWebhookLog;
import com.elingo.premium.gateway.GatewayRegistry;
import com.elingo.premium.gateway.InvalidSignatureException;
import com.elingo.premium.gateway.PaymentGateway;
import com.elingo.premium.gateway.WebhookEvent;
import com.elingo.premium.repository.PaymentProviderRepository;
import com.elingo.premium.service.PaymentProcessingService;
import com.elingo.premium.service.WebhookLogWriter;
import com.elingo.premium.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "WEBHOOK-SERVICE")
public class WebhookServiceImpl implements WebhookService {

    private final PaymentProviderRepository providerRepository;
    private final GatewayRegistry gatewayRegistry;
    private final WebhookLogWriter webhookLogWriter;
    private final PaymentProcessingService paymentProcessingService;

    @Override
    public void handle(String providerCode, Map<String, String> headers, String rawBody) {
        log.info("Received payment webhook providerCode={}", providerCode);

        // 1. Tìm thông tin provider
        PaymentProvider provider = providerRepository.findByCodeIgnoreCase(providerCode)
                .orElseThrow(() -> new AppException(AppError.PAYMENT_PROVIDER_NOT_FOUND));

        if (Boolean.FALSE.equals(provider.getIsActive())) {
            log.warn("Payment provider is inactive providerCode={}", providerCode);
            throw new AppException(AppError.PAYMENT_PROVIDER_INACTIVE);
        }

        // 2. Lấy gateway xử lý tương ứng
        PaymentGateway gateway = gatewayRegistry.getGateway(providerCode);

        // 3. parseAndVerify - sai xác thực thì lưu log signature_valid=false và trả 401
        WebhookEvent event;
        try {
            event = gateway.parseAndVerify(headers, rawBody);
        } catch (InvalidSignatureException e) {
            log.warn("Webhook verification failed for providerCode={}", providerCode);
            webhookLogWriter.saveReceived(provider, null, rawBody, false);
            throw new AppException(AppError.INVALID_PAYMENT_SIGNATURE);
        }

        // 4. Ghi log RECEIVED (REQUIRES_NEW). Vi phạm unique coi là trùng, trả 200 và bỏ qua
        Optional<PaymentWebhookLog> logOpt = webhookLogWriter.saveReceived(
                provider,
                event.eventId(),
                rawBody,
                true
        );

        if (logOpt.isEmpty()) {
            log.info("Duplicate webhook event ignored providerCode={} eventId={}",
                    providerCode, event.eventId());
            return;
        }

        PaymentWebhookLog savedLog = logOpt.get();

        // 5. Xử lý nghiệp vụ thanh toán và cập nhật log (markDone/markFailed)
        try {
            PaymentProcessingResult result = paymentProcessingService.process(event, provider);
            webhookLogWriter.markDone(savedLog.getId(), result.transactionId(), result.status());
        } catch (Exception ex) {
            log.error("Internal error processing payment event providerCode={} eventId={} refCode={}",
                    providerCode, event.eventId(), event.refCode(), ex);
            webhookLogWriter.markFailed(savedLog.getId(), null);
        }
    }
}
