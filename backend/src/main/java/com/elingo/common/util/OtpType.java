package com.elingo.common.util;

import lombok.Getter;

@Getter
public enum OtpType {
    VERIFY_ACCOUNT("verify-account", "Verify Account"),
    RESET_PASSWORD("reset-password", "Reset Password"),
    CHANGE_EMAIL("change-email", "Verify Email For Email Update");

    private final String prefix;
    private final String title;

    OtpType(String prefix, String title) {
        this.prefix = prefix;
        this.title = title;
    }
}
