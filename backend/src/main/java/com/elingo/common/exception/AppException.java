package com.elingo.common.exception;

public class AppException extends RuntimeException {
    public AppException(AppError appError) {
        super(appError.getMessage());
        this.appError = appError;
    }

    private final AppError appError;

    public AppError getAppError() {
        return appError;
    }
}
