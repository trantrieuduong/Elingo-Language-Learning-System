package com.elingo.premium.controller;

import com.elingo.common.dto.ApiResponse;
import com.elingo.premium.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/webhooks/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Webhook Controller")
public class PaymentWebhookController {

    private final WebhookService webhookService;

    @PostMapping("/{providerCode}")
    @Operation(summary = "Handle payment webhook from provider")
    public ApiResponse<Void> handleWebhook(
            @PathVariable String providerCode,
            @RequestHeader Map<String, String> headers,
            @RequestBody String rawBody
    ) {
        webhookService.handle(providerCode, headers, rawBody);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
