package com.elingo.premium.service;

import java.util.Map;

public interface WebhookService {
    void handle(String providerCode, Map<String, String> headers, String rawBody);
}
