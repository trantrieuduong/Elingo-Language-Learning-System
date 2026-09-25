package com.elingo.learning.flashcard.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class GradeValidator implements ConstraintValidator<ValidGrade, Integer> {

    @Override
    public boolean isValid(Integer grade, ConstraintValidatorContext context) {
        if (grade == null) return false;
        return grade >= 0 && grade <= 3;
    }
}
