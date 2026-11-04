package com.elingo.premium.gateway;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GatewayRegistry {
    private final Map<String, PaymentGateway> gatewayMap;

    public GatewayRegistry(List<PaymentGateway> gateways) {
        this.gatewayMap = gateways.stream()
                .collect(Collectors.toMap(
                        gw -> gw.code().toUpperCase(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    public PaymentGateway getGateway(String code) {
        if (code == null || code.isBlank()) {
            throw new AppException(AppError.PAYMENT_PROVIDER_NOT_FOUND);
        }
        PaymentGateway gateway = gatewayMap.get(code.toUpperCase());
        if (gateway == null) {
            throw new AppException(AppError.PAYMENT_PROVIDER_NOT_FOUND);
        }
        return gateway;
    }
}
