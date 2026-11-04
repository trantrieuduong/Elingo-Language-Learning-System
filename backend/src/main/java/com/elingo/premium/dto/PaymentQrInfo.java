package com.elingo.premium.dto;

public record PaymentQrInfo(
        String qrCodeUrl,
        String bankName,
        String bankAccount,
        String bankAccountName
) {
}

