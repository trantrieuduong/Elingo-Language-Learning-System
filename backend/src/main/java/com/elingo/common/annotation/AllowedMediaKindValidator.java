package com.elingo.common.annotation;

import com.elingo.common.enums.MediaKind;
import com.elingo.file.util.FileKey;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public class AllowedMediaKindValidator implements ConstraintValidator<AllowedMediaKind, String> {

    private Set<MediaKind> allowed;

    @Override
    public void initialize(AllowedMediaKind constraintAnnotation) {
        if (constraintAnnotation.value().length == 0) {
            throw new IllegalArgumentException("@AllowedMediaKind required at least 1 MediaKind");
        }
        this.allowed = EnumSet.copyOf(Arrays.asList(constraintAnnotation.value()));
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        MediaKind kind = FileKey.mediaKindOf(value);
        return kind != null && allowed.contains(kind);
    }
}
