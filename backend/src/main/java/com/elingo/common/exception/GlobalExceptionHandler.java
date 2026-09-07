package com.elingo.common.exception;

import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.ErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j(topic = "GLOBAL-EXCEPTION-HANDLER")
public class GlobalExceptionHandler {

    /*
     * Unhandled errors during runtime
     */
    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected error occurred", ex);
        AppError appError = AppError.UNCATEGORIZED_EXCEPTION;
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .errors(List.of(ErrorDetail.builder()
                                .code(appError.getCode())
                                .message(appError.getMessage())
                                .build()))
                        .build());
    }

    /*
     * Handled business errors
     */
    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<?>> handleAppException(AppException ex) {
        AppError appError = ex.getAppError();
        log.error("Business error: {} - {}", appError.getCode(), ex.getMessage());
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .errors(List.of(ErrorDetail.builder()
                                .code(appError.getCode())
                                .message(appError.getMessage())
                                .build()))
                        .build());
    }

    /*
     * Exception from annotations validation (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> {
                    String code = fieldError.getDefaultMessage();
                    String message;
                    try {
                        message = AppError.valueOf(code).getMessage();
                    } catch (Exception e) {
                        message = fieldError.getDefaultMessage();
                    }
                    return ErrorDetail.builder()
                            .field(fieldError.getField())
                            .code(code)
                            .message(message)
                            .build();
                })
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.builder()
                        .success(false)
                        .errors(errors)
                        .build());
    }

    /*
     * Database unique constraint violation exception
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<?>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        AppError appError = AppError.UNCATEGORIZED_EXCEPTION;

        if (cause instanceof ConstraintViolationException cve) {
            String constraint = cve.getConstraintName();
            if (constraint != null) {
                if (constraint.contains("uk_user_username")) {
                    appError = AppError.USERNAME_EXISTED;
                } else if (constraint.contains("uk_user_email")) {
                    appError = AppError.EMAIL_EXISTED;
                } else if (constraint.contains("uk_deck_slug")) {
                    appError = AppError.DECK_SLUG_EXISTED;
                } else if (constraint.contains("uk_topic_slug_per_deck")) {
                    appError = AppError.TOPIC_SLUG_EXISTED;
                } else if (constraint.contains("uk_lesson_slug")) {
                    appError = AppError.LESSON_SLUG_EXISTED;
                } else if (constraint.contains("uk_tag_code")) {
                    appError = AppError.TAG_CODE_EXISTED;
                } else if (constraint.contains("uk_cefr_level_code")) {
                    appError = AppError.CEFR_LEVEL_CODE_EXISTED;
                } else if (constraint.contains("uk_badge_code")) {
                    appError = AppError.BADGE_CODE_EXISTED;
                } else if (constraint.contains("uk_user_checkin_date")) {
                    appError = AppError.ALREADY_CHECKED_IN_TODAY;
                }
            }
        }

        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .errors(List.of(ErrorDetail.builder()
                                .code(appError.getCode())
                                .message(appError.getMessage())
                                .build()))
                        .build());
    }
}
