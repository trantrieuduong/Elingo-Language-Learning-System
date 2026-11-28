package com.elingo.auth.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    // BCrypt chỉ xử lý tối đa 72 byte
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$";

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return false;
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) return false;
        return password.matches(PASSWORD_REGEX);
    }
}
