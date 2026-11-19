package com.elingo.premium.service.impl;

import com.elingo.common.enums.SystemConfigKey;
import com.elingo.common.service.SystemConfigService;
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

    private static final String DEFAULT_BANK_NAME = "MBBank";
    private static final String DEFAULT_QR_TEMPLATE = "compact";

    private final SystemConfigService systemConfigService;

    @Override
    public PaymentQrInfo generateQrInfo(BigDecimal amount, String currency, String refCode) {
        String bankName = systemConfigService.getString(SystemConfigKey.BANK_NAME, DEFAULT_BANK_NAME);
        String bankAccount = systemConfigService.getString(SystemConfigKey.BANK_ACCOUNT, "");
        String bankAccountName = systemConfigService.getString(SystemConfigKey.BANK_ACCOUNT_NAME, "");
        String qrTemplate = systemConfigService.getString(SystemConfigKey.BANK_QR_TEMPLATE, DEFAULT_QR_TEMPLATE);

        long amountLong = (amount != null) ? amount.longValue() : 0L;

        String qrCodeUrl = String.format("https://vietqr.app/img?acc=%s&bank=%s&amount=%d&des=%s&template=%s",
                URLEncoder.encode(bankAccount != null ? bankAccount : "", StandardCharsets.UTF_8),
                URLEncoder.encode(bankName != null ? bankName : "", StandardCharsets.UTF_8),
                amountLong,
                URLEncoder.encode(refCode != null ? refCode : "", StandardCharsets.UTF_8),
                URLEncoder.encode(qrTemplate != null ? qrTemplate : DEFAULT_QR_TEMPLATE, StandardCharsets.UTF_8)
        );

        return new PaymentQrInfo(
                qrCodeUrl,
                bankName,
                bankAccount,
                bankAccountName
        );
    }
}
