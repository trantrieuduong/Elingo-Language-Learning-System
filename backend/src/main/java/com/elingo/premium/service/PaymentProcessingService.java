package com.elingo.premium.service;

import com.elingo.premium.dto.PaymentProcessingResult;
import com.elingo.premium.entity.PaymentProvider;
import com.elingo.premium.gateway.WebhookEvent;

public interface PaymentProcessingService {
    PaymentProcessingResult process(WebhookEvent event, PaymentProvider provider);
}
