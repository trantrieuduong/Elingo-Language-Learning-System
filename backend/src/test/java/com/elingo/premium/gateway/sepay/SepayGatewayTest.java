package com.elingo.premium.gateway.sepay;

import com.elingo.premium.config.PremiumProperties;
import com.elingo.premium.gateway.InvalidSignatureException;
import com.elingo.premium.gateway.WebhookEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SepayGatewayTest {

    @Mock
    private PremiumProperties premiumProperties;

    private ObjectMapper objectMapper;
    private SepayGateway sepayGateway;

    private static final String SECRET_KEY = "test-sepay-secret-key";
    private PremiumProperties.Sepay sepayConfig;
    private PremiumProperties.BankTransfer bankTransferConfig;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sepayGateway = new SepayGateway(premiumProperties, objectMapper);

        sepayConfig = new PremiumProperties.Sepay();
        sepayConfig.setSecretKey(SECRET_KEY);

        bankTransferConfig = new PremiumProperties.BankTransfer();
        bankTransferConfig.setCodePrefix("ELGO");
    }

    private String calculateHmac(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("code() should return SEPAY")
    void code_ReturnsSepay() {
        assertThat(sepayGateway.code()).isEqualTo("SEPAY");
    }

    @Nested
    @DisplayName("parseAndVerify tests")
    class ParseAndVerifyTests {

        @Test
        @DisplayName("Should parse and verify webhook successfully when signature and payload are valid")
        void parseAndVerify_Success_WhenSignatureValid() throws Exception {
            when(premiumProperties.getSepay()).thenReturn(sepayConfig);

            long nowSeconds = Instant.now().getEpochSecond();
            String rawBody = """
                    {
                      "id": 123456,
                      "transferType": "in",
                      "transferAmount": 99000,
                      "code": "ELGO_ABC123",
                      "content": "ELGO_ABC123 thanh toan"
                    }
                    """;

            String payloadToSign = nowSeconds + "." + rawBody;
            String signature = "sha256=" + calculateHmac(payloadToSign, SECRET_KEY);

            Map<String, String> headers = Map.of(
                    "X-SePay-Timestamp", String.valueOf(nowSeconds),
                    "X-SePay-Signature", signature
            );

            WebhookEvent event = sepayGateway.parseAndVerify(headers, rawBody);

            assertThat(event).isNotNull();
            assertThat(event.eventId()).isEqualTo("123456");
            assertThat(event.refCode()).isEqualTo("ELGO_ABC123");
            assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("99000"));
            assertThat(event.success()).isTrue();
        }

        @Test
        @DisplayName("Should extract refCode from content field when code field is null")
        void parseAndVerify_ExtractsRefCodeFromContent_WhenCodeIsNull() throws Exception {
            when(premiumProperties.getSepay()).thenReturn(sepayConfig);
            when(premiumProperties.getBankTransfer()).thenReturn(bankTransferConfig);

            long nowSeconds = Instant.now().getEpochSecond();
            String rawBody = """
                    {
                      "id": 78910,
                      "transferType": "in",
                      "transferAmount": 199000,
                      "content": "Chuyen tien elgo_xyz999 qua app"
                    }
                    """;

            String payloadToSign = nowSeconds + "." + rawBody;
            String signature = "sha256=" + calculateHmac(payloadToSign, SECRET_KEY);

            Map<String, String> headers = Map.of(
                    "X-SePay-Timestamp", String.valueOf(nowSeconds),
                    "X-SePay-Signature", signature
            );

            WebhookEvent event = sepayGateway.parseAndVerify(headers, rawBody);

            assertThat(event).isNotNull();
            assertThat(event.eventId()).isEqualTo("78910");
            assertThat(event.refCode()).isEqualTo("ELGO_XYZ999"); // chuẩn hóa uppercase
            assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("199000"));
            assertThat(event.success()).isTrue();
        }

        @Test
        @DisplayName("Should throw InvalidSignatureException when X-SePay-Signature header is missing")
        void parseAndVerify_ThrowsException_WhenSignatureHeaderMissing() {
            Map<String, String> headers = Map.of("X-SePay-Timestamp", "1700000000");

            assertThatThrownBy(() -> sepayGateway.parseAndVerify(headers, "{}"))
                    .isInstanceOf(InvalidSignatureException.class)
                    .hasMessageContaining("Missing X-SePay-Signature header");
        }

        @Test
        @DisplayName("Should throw InvalidSignatureException when X-SePay-Timestamp header is missing")
        void parseAndVerify_ThrowsException_WhenTimestampHeaderMissing() {
            Map<String, String> headers = Map.of("X-SePay-Signature", "sha256=abcdef");

            assertThatThrownBy(() -> sepayGateway.parseAndVerify(headers, "{}"))
                    .isInstanceOf(InvalidSignatureException.class)
                    .hasMessageContaining("Missing X-SePay-Timestamp header");
        }

        @Test
        @DisplayName("Should throw InvalidSignatureException when timestamp is expired")
        void parseAndVerify_ThrowsException_WhenTimestampExpired() {
            long expiredTimestamp = Instant.now().getEpochSecond() - 300; // 5 phút trước
            Map<String, String> headers = Map.of(
                    "X-SePay-Timestamp", String.valueOf(expiredTimestamp),
                    "X-SePay-Signature", "sha256=dummy"
            );

            assertThatThrownBy(() -> sepayGateway.parseAndVerify(headers, "{}"))
                    .isInstanceOf(InvalidSignatureException.class)
                    .hasMessageContaining("Webhook request expired");
        }

        @Test
        @DisplayName("Should throw InvalidSignatureException when HMAC signature mismatches")
        void parseAndVerify_ThrowsException_WhenSignatureMismatch() {
            when(premiumProperties.getSepay()).thenReturn(sepayConfig);

            long nowSeconds = Instant.now().getEpochSecond();
            Map<String, String> headers = Map.of(
                    "X-SePay-Timestamp", String.valueOf(nowSeconds),
                    "X-SePay-Signature", "sha256=invalidhash123456"
            );

            assertThatThrownBy(() -> sepayGateway.parseAndVerify(headers, "{}"))
                    .isInstanceOf(InvalidSignatureException.class)
                    .hasMessageContaining("Invalid webhook signature");
        }
    }
}
