package com.elingo.premium.service;

import com.elingo.premium.config.PremiumProperties;
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
    private PremiumProperties premiumProperties;

    private VietQrServiceImpl vietQrService;
    private PremiumProperties.BankTransfer bankTransferConfig;

    @BeforeEach
    void setUp() {
        vietQrService = new VietQrServiceImpl(premiumProperties);

        bankTransferConfig = new PremiumProperties.BankTransfer();
        bankTransferConfig.setCodePrefix("ELGO");
        bankTransferConfig.setBankName("MBBank");
        bankTransferConfig.setBankAccount("0123456789");
        bankTransferConfig.setBankAccountName("CONG TY ELINGO");
        bankTransferConfig.setQrTemplate("compact");
    }

    @Test
    @DisplayName("Should generate correct VietQR URL and account info with all parameters")
    void generateQrInfo_ReturnsCorrectInfo() {
        when(premiumProperties.getBankTransfer()).thenReturn(bankTransferConfig);

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
    @DisplayName("Should safely handle null amount and fall back to default template")
    void generateQrInfo_HandlesNullAmountAndNullTemplate() {
        bankTransferConfig.setQrTemplate(null);
        when(premiumProperties.getBankTransfer()).thenReturn(bankTransferConfig);

        String refCode = "ELGO_DEFAULT";

        PaymentQrInfo qrInfo = vietQrService.generateQrInfo(null, "VND", refCode);

        assertThat(qrInfo).isNotNull();
        assertThat(qrInfo.qrCodeUrl()).contains("amount=0");
        assertThat(qrInfo.qrCodeUrl()).contains("template=compact");
    }
}
