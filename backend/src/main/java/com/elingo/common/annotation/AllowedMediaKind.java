package com.elingo.common.annotation;

import com.elingo.common.enums.MediaKind;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Constraint(validatedBy = AllowedMediaKindValidator.class)
public @interface AllowedMediaKind {
    /** Các nhóm định dạng được chấp nhận cho key này. */
    MediaKind[] value();
    String message() default "INVALID_FILE_TYPE";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
