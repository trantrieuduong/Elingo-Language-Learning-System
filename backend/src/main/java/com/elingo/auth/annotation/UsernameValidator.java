package com.elingo.auth.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private static final String USERNAME_REGEX = "^[a-zA-Z0-9]{3,15}$";

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null) return false;
        return username.matches(USERNAME_REGEX);
    }
}
