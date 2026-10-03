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
    ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Request rejected errorCode={} method={}", AppError.METHOD_NOT_SUPPORTED.getCode(), ex.getMethod());
        AppError appError = AppError.METHOD_NOT_SUPPORTED;
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
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
    ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Request rejected errorCode={} contentType={}",
                AppError.MEDIA_TYPE_NOT_SUPPORTED.getCode(), ex.getContentType());
        AppError appError = AppError.MEDIA_TYPE_NOT_SUPPORTED;
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
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
    ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        log.warn("Request rejected errorCode={}", AppError.MEDIA_TYPE_NOT_ACCEPTABLE.getCode());
        AppError appError = AppError.MEDIA_TYPE_NOT_ACCEPTABLE;
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
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
    ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Request rejected errorCode={} errorType={}",
                AppError.MALFORMED_JSON.getCode(), ex.getClass().getSimpleName());
        AppError appError = AppError.MALFORMED_JSON;
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
     * Missing required query parameter (400)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        log.warn("Request rejected errorCode={} parameter={}",
                AppError.PARAM_MISSING.getCode(), ex.getParameterName());
        AppError appError = AppError.PARAM_MISSING;
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
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
    ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Request rejected errorCode={} parameter={}",
                AppError.PARAM_TYPE_MISMATCH.getCode(), ex.getName());
        AppError appError = AppError.PARAM_TYPE_MISMATCH;
        String message = String.format("Parameter '%s' should be of type '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
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
    ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("Request rejected errorCode={} path={}",
                AppError.RESOURCE_NOT_FOUND.getCode(), ex.getResourcePath());
        AppError appError = AppError.RESOURCE_NOT_FOUND;
        return ResponseEntity.status(appError.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
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
    ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("Request rejected errorCode={}", AppError.FILE_TOO_LARGE.getCode());
        AppError appError = AppError.FILE_TOO_LARGE;
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
