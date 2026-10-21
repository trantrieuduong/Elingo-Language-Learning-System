package com.elingo.common.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class SlugValidator implements ConstraintValidator<ValidSlug, String> {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    @Override
    public boolean isValid(String slug, ConstraintValidatorContext context) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        return SLUG_PATTERN.matcher(slug).matches();
    }
}