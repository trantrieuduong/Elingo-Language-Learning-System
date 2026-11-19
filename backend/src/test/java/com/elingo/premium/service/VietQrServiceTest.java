package com.elingo.premium.service;

import com.elingo.common.enums.SystemConfigKey;
import com.elingo.common.service.SystemConfigService;
import com.elingo.premium.dto.PaymentQrInfo;
import com.elingo.premium.service.impl.VietQrServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class VietQrServiceTest {

    @Mock
    private SystemConfigService systemConfigService;

    private VietQrServiceImpl vietQrService;

    @BeforeEach
    void setUp() {
        vietQrService = new VietQrServiceImpl(systemConfigService);
    }

    @Test
    @DisplayName("Should generate correct VietQR URL and account info with all parameters")
    void generateQrInfo_ReturnsCorrectInfo() {
        when(systemConfigService.getString(SystemConfigKey.BANK_NAME, "MBBank")).thenReturn("MBBank");
        when(systemConfigService.getString(SystemConfigKey.BANK_ACCOUNT, "")).thenReturn("0123456789");
        when(systemConfigService.getString(SystemConfigKey.BANK_ACCOUNT_NAME, "")).thenReturn("CONG TY ELINGO");
        when(systemConfigService.getString(SystemConfigKey.BANK_QR_TEMPLATE, "compact")).thenReturn("compact");

        BigDecimal amount = new BigDecimal("99000");
        String refCode = "ELGO_M1N2O3P4";

        PaymentQrInfo qrInfo = vietQrService.generateQrInfo(amount, "VND", refCode);

        assertThat(qrInfo).isNotNull();
        assertThat(qrInfo.bankName()).isEqualTo("MBBank");
        assertThat(qrInfo.bankAccount()).isEqualTo("0123456789");
        assertThat(qrInfo.bankAccountName()).isEqualTo("CONG TY ELINGO");
        assertThat(qrInfo.qrCodeUrl()).contains("https://vietqr.app/img?");
        assertThat(qrInfo.qrCodeUrl()).contains("acc=0123456789");
        assertThat(qrInfo.qrCodeUrl()).contains("bank=MBBank");
        assertThat(qrInfo.qrCodeUrl()).contains("amount=99000");
        assertThat(qrInfo.qrCodeUrl()).contains("des=ELGO_M1N2O3P4");
        assertThat(qrInfo.qrCodeUrl()).contains("template=compact");
    }

    @Test
    @DisplayName("Should fallback to default values when configs are blank")
    void generateQrInfo_FallbackDefaults() {
        when(systemConfigService.getString(SystemConfigKey.BANK_NAME, "MBBank")).thenReturn("MBBank");
        when(systemConfigService.getString(SystemConfigKey.BANK_ACCOUNT, "")).thenReturn("");
        when(systemConfigService.getString(SystemConfigKey.BANK_ACCOUNT_NAME, "")).thenReturn("");
        when(systemConfigService.getString(SystemConfigKey.BANK_QR_TEMPLATE, "compact")).thenReturn("compact");

        PaymentQrInfo qrInfo = vietQrService.generateQrInfo(null, "VND", null);

        assertThat(qrInfo).isNotNull();
        assertThat(qrInfo.bankName()).isEqualTo("MBBank");
        assertThat(qrInfo.bankAccount()).isEqualTo("");
        assertThat(qrInfo.bankAccountName()).isEqualTo("");
        assertThat(qrInfo.qrCodeUrl()).contains("amount=0");
    }
}
