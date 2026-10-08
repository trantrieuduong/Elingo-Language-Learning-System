package com.elingo.learning.flashcard.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GradeValidator.class)
public @interface ValidGrade {
    String message() default "INVALID_SRS_GRADE";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
