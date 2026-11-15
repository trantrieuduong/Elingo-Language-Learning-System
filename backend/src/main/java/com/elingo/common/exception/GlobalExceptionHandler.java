package com.elingo.common.exception;

import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.ErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import static java.util.Map.entry;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j(topic = "GLOBAL-EXCEPTION-HANDLER")
public class GlobalExceptionHandler {

    private static final Map<String, AppError> CONSTRAINT_ERROR_MAPPINGS = Map.ofEntries(
            entry("uk_user_username",       AppError.USERNAME_EXISTED),
            entry("uk_user_email",          AppError.EMAIL_EXISTED),
            entry("uk_deck_slug",           AppError.DECK_SLUG_EXISTED),
            entry("uk_topic_slug_per_deck", AppError.TOPIC_SLUG_EXISTED),
            entry("uk_lesson_slug",         AppError.LESSON_SLUG_EXISTED),
            entry("uk_badge_code",          AppError.BADGE_CODE_EXISTED),
            entry("uk_user_checkin_date",   AppError.ALREADY_CHECKED_IN_TODAY),
            entry("uk_user_segment",        AppError.PROGRESS_UPDATE_CONFLICT),
            entry("uk_user_lesson",         AppError.PROGRESS_UPDATE_CONFLICT)
    );

    /*
     * Access denied / forbidden error (403)
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        AppError appError = AppError.UNAUTHORIZED;
        log.warn("Access denied code={}", appError.getCode());
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .errors(List.of(ErrorDetail.builder()
                                .code(appError.getCode())
                                .message(appError.getMessage())
                                .build()))
                        .build());
    }

    /*
     * Unhandled errors during runtime
     */
    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled runtime error errorType={}", ex.getClass().getSimpleName(), ex);
        AppError appError = AppError.UNCATEGORIZED_EXCEPTION;
        return buildErrorResponse(appError);
    }

    /*
     * Handled business errors
     */
    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        AppError appError = ex.getAppError();
        log.warn("Business error code={}", appError.getCode());
        return buildErrorResponse(appError);
    }

    /*
     * Exception from annotations validation (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
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

        log.info("Validation failed fields={}",
                ex.getBindingResult().getFieldErrors().stream().map(FieldError::getField).toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .errors(errors)
                        .build());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        String code = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("INVALID_REQUEST");

        return buildErrorResponse(AppError.valueOf(code));
    }

    /*
     * Database unique constraint violation exception
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        AppError appError = resolveConstraintViolation(ex.getCause());

        if (appError == AppError.UNCATEGORIZED_EXCEPTION) {
            log.error("Unmapped data integrity violation", ex);
        } else {
            log.warn("Data integrity violation code={}", appError.getCode());
        }

        return buildErrorResponse(appError);
    }

    private AppError resolveConstraintViolation(Throwable cause) {
        if (!(cause instanceof ConstraintViolationException cve)) {
            return AppError.UNCATEGORIZED_EXCEPTION;
        }

        String constraint = cve.getConstraintName();
        if (constraint == null) {
            return AppError.UNCATEGORIZED_EXCEPTION;
        }

        String normalized = constraint.toLowerCase();
        return CONSTRAINT_ERROR_MAPPINGS.entrySet().stream()
                .filter(e -> normalized.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(AppError.UNCATEGORIZED_EXCEPTION);
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(AppError appError) {
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .errors(List.of(ErrorDetail.builder()
                                .code(appError.getCode())
                                .message(appError.getMessage())
                                .build()))
                        .build());
    }
}
