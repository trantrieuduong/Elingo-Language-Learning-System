package com.elingo.premium.service.impl;

import com.elingo.premium.config.PremiumProperties;
import com.elingo.premium.dto.PaymentQrInfo;
import com.elingo.premium.service.PaymentQrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-QR-SERVICE")
public class VietQrServiceImpl implements PaymentQrService {

    private final PremiumProperties premiumProperties;

    @Override
    public PaymentQrInfo generateQrInfo(BigDecimal amount, String currency, String refCode) {
        String bankName = premiumProperties.getBankTransfer().getBankName();
        String bankAccount = premiumProperties.getBankTransfer().getBankAccount();
        String bankAccountName = premiumProperties.getBankTransfer().getBankAccountName();
        String qrTemplate = premiumProperties.getBankTransfer().getQrTemplate();

        long amountLong = (amount != null) ? amount.longValue() : 0L;

        String qrCodeUrl = String.format("https://vietqr.app/img?acc=%s&bank=%s&amount=%d&des=%s&template=%s",
                URLEncoder.encode(bankAccount != null ? bankAccount : "", StandardCharsets.UTF_8),
                URLEncoder.encode(bankName != null ? bankName : "", StandardCharsets.UTF_8),
                amountLong,
                URLEncoder.encode(refCode != null ? refCode : "", StandardCharsets.UTF_8),
                URLEncoder.encode(qrTemplate != null ? qrTemplate : "compact", StandardCharsets.UTF_8)
        );

        return new PaymentQrInfo(
                qrCodeUrl,
                bankName,
                bankAccount,
                bankAccountName
        );
    }
}
