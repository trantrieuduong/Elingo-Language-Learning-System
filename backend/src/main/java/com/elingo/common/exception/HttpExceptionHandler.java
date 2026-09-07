package com.elingo.common.exception;

import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.ErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j(topic = "HTTP-EXCEPTION-HANDLER")
public class HttpExceptionHandler {

    /*
     * HTTP Method not supported (405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<?>> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMessage());
        AppError appError = AppError.METHOD_NOT_SUPPORTED;
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .errors(List.of(ErrorDetail.builder()
                                .code(appError.getCode())
                                .message(ex.getMessage())
                                .build()))
                        .build());
    }

    /*
     * Content-Type not supported (415)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResponse<?>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Media type not supported: {}", ex.getMessage());
        AppError appError = AppError.MEDIA_TYPE_NOT_SUPPORTED;
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .errors(List.of(ErrorDetail.builder()
                                .code(appError.getCode())
                                .message(ex.getMessage())
                                .build()))
                        .build());
    }

    /*
     * Accept header not acceptable (406)
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ApiResponse<?>> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        log.warn("Media type not acceptable: {}", ex.getMessage());
        AppError appError = AppError.MEDIA_TYPE_NOT_ACCEPTABLE;
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .errors(List.of(ErrorDetail.builder()
                                .code(appError.getCode())
                                .message(ex.getMessage())
                                .build()))
                        .build());
    }

    /*
     * Request body unreadable, malformed JSON, or deserialization error (400)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request body: {}", ex.getMessage());
        AppError appError = AppError.MALFORMED_JSON;
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
     * Missing required query parameter (400)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<?>> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        AppError appError = AppError.PARAM_MISSING;
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .errors(List.of(ErrorDetail.builder()
                                .field(ex.getParameterName())
                                .code(appError.getCode())
                                .message(ex.getMessage())
                                .build()))
                        .build());
    }

    /*
     * Parameter type mismatch on URL or query params (400)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Parameter type mismatch for '{}': {}", ex.getName(), ex.getMessage());
        AppError appError = AppError.PARAM_TYPE_MISMATCH;
        String message = String.format("Parameter '%s' should be of type '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .errors(List.of(ErrorDetail.builder()
                                .field(ex.getName())
                                .code(appError.getCode())
                                .message(message)
                                .build()))
                        .build());
    }

    /*
     * Resource or route not found (404)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<?>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getResourcePath());
        AppError appError = AppError.RESOURCE_NOT_FOUND;
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.builder()
                        .success(false)
                        .errors(List.of(ErrorDetail.builder()
                                .code(appError.getCode())
                                .message(ex.getMessage())
                                .build()))
                        .build());
    }

    /*
     * Upload file exceeds configured size limit (400)
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<?>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("Upload file size exceeded: {}", ex.getMessage());
        AppError appError = AppError.FILE_TOO_LARGE;
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
