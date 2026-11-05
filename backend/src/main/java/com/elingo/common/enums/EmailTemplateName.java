package com.elingo.common.enums;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
    SEND_OTP("send_otp"),
    PAYMENT_SUCCESS("payment_success");

    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }
}