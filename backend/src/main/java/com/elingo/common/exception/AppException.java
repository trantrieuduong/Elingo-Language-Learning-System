package com.elingo.common.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    public AppException(AppError appError) {
        super(appError.getMessage());
        this.appError = appError;
    }

    private final AppError appError;
}
