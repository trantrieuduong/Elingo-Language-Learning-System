package com.elingo.common.util;

public enum OtpType {
    VERIFY_ACCOUNT("verify-account"),
    RESET_PASSWORD("reset-password"),
    CHANGE_EMAIL("change-email");

    private final String prefix;

    OtpType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() { return prefix; }
}
