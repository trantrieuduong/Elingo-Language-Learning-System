package com.elingo.premium.service;

import com.elingo.premium.dto.PaymentQrInfo;

import java.math.BigDecimal;

public interface PaymentQrService {
    PaymentQrInfo generateQrInfo(BigDecimal amount, String currency, String refCode);
}
