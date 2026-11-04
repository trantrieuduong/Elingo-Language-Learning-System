package com.elingo.premium.gateway;

import java.util.Map;

public interface PaymentGateway {
    String code();

    WebhookEvent parseAndVerify(Map<String, String> headers, String rawBody) throws InvalidSignatureException, IllegalArgumentException;
}
