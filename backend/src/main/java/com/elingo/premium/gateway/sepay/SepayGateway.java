package com.elingo.premium.gateway.sepay;

import com.elingo.premium.config.PremiumProperties;
import com.elingo.premium.gateway.InvalidSignatureException;
import com.elingo.premium.gateway.PaymentGateway;
import com.elingo.premium.gateway.WebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "SEPAY-GATEWAY")
public class SepayGateway implements PaymentGateway {

    private static final String GATEWAY_CODE = "SEPAY";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-SePay-Signature";
    private static final String TIMESTAMP_HEADER = "X-SePay-Timestamp";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final long MAX_ALLOWED_TIMESTAMP_DRIFT_SECONDS = 120; // 2 phút

    private final PremiumProperties premiumProperties;
    private final ObjectMapper objectMapper;

    @Override
    public String code() {
        return GATEWAY_CODE;
    }

    @Override
    public WebhookEvent parseAndVerify(Map<String, String> headers, String rawBody) throws InvalidSignatureException, IllegalArgumentException {
        verifyHmacSignature(headers, rawBody);

        try {
            JsonNode root = objectMapper.readTree(rawBody);

            String eventId = null;
            if (root.hasNonNull("id")) {
                eventId = root.path("id").asString();
            }

            String transferType = root.path("transferType").asString("");
            boolean success = "in".equalsIgnoreCase(transferType);

            BigDecimal amount = BigDecimal.ZERO;
            if (root.hasNonNull("transferAmount")) {
                amount = new BigDecimal(root.path("transferAmount").asString("0"));
            }

            String code = null;
            if (root.hasNonNull("code")) {
                String c = root.path("code").asString().trim();
                if (!c.isBlank()) {
                    code = c;
                }
            }

            String content = root.path("content").asString("");
            String refCode = code;
            if (refCode == null && !content.isBlank()) {
                refCode = extractRefCodeFromContent(content);
            }

            if (refCode != null) {
                refCode = refCode.trim().toUpperCase();
            }

            log.info("Parsed SePay webhook eventId={} refCode={} amount={} success={}",
                    eventId, refCode, amount, success);

            return new WebhookEvent(eventId, refCode, amount, success, rawBody);
        } catch (Exception e) {
            log.error("Failed to parse SePay webhook payload: {}", e.getMessage());
            throw new IllegalArgumentException("Malformed SePay webhook payload", e);
        }
    }

    private void verifyHmacSignature(Map<String, String> headers, String rawBody) throws InvalidSignatureException {
        String signatureHeader = getHeaderCaseInsensitive(headers, SIGNATURE_HEADER);
        String timestampHeader = getHeaderCaseInsensitive(headers, TIMESTAMP_HEADER);

        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Missing {} header in SePay webhook request", SIGNATURE_HEADER);
            throw new InvalidSignatureException("Missing " + SIGNATURE_HEADER + " header");
        }

        if (timestampHeader == null || timestampHeader.isBlank()) {
            log.warn("Missing {} header in SePay webhook request", TIMESTAMP_HEADER);
            throw new InvalidSignatureException("Missing " + TIMESTAMP_HEADER + " header");
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp format in {}: {}", TIMESTAMP_HEADER, timestampHeader);
            throw new InvalidSignatureException("Invalid " + TIMESTAMP_HEADER + " format");
        }

        long currentSeconds = Instant.now().getEpochSecond();
        if (Math.abs(currentSeconds - timestamp) > MAX_ALLOWED_TIMESTAMP_DRIFT_SECONDS) {
            log.warn("SePay webhook timestamp expired current={} received={}", currentSeconds, timestamp);
            throw new InvalidSignatureException("Webhook request expired");
        }

        String secretKey = premiumProperties.getSepay().getSecretKey();
        if (secretKey == null || secretKey.isBlank()) {
            log.error("SePay webhook secret key is not configured");
            throw new InvalidSignatureException("SePay webhook secret not configured");
        }

        String payloadToSign = timestamp + "." + (rawBody != null ? rawBody : "");
        String calculatedHash = calculateHmacSha256(payloadToSign, secretKey);
        String expectedSignature = SIGNATURE_PREFIX + calculatedHash;

        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = signatureHeader.trim().getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
            log.warn("SePay webhook HMAC signature verification failed");
            throw new InvalidSignatureException("Invalid webhook signature");
        }
    }

    private String calculateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", e);
        }
    }

    private String getHeaderCaseInsensitive(Map<String, String> headers, String headerName) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (headerName.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String extractRefCodeFromContent(String content) {
        String prefix = premiumProperties.getBankTransfer().getCodePrefix();
        if (prefix == null || prefix.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile("(?i)\\b(" + Pattern.quote(prefix) + "[A-Za-z0-9_-]+)\\b");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
