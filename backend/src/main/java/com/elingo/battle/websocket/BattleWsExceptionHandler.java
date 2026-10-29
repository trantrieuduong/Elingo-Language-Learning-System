package com.elingo.battle.websocket;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.security.Principal;

/**
 * Bắt exception từ @MessageMapping handlers.
 */
@Slf4j(topic = "BATTLE-WS-EXCEPTION-HANDLER")
@ControllerAdvice
@RequiredArgsConstructor
public class BattleWsExceptionHandler {

    private final BattleEventPublisher eventPublisher;

    @MessageExceptionHandler(AppException.class)
    public void handleAppException(AppException ex, Principal principal) {
        if (principal == null) return;
        Long userId = Long.parseLong(principal.getName());
        log.warn("Battle business error userId={} code={}", userId, ex.getAppError().getCode());
        eventPublisher.sendError(userId, ex.getAppError().getCode(), ex.getMessage());
    }

    // Payload @Valid thất bại trên STOMP handler
    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidation(MethodArgumentNotValidException ex, Principal principal) {
        if (principal == null) return;
        Long userId = Long.parseLong(principal.getName());
        log.warn("Battle payload validation failed userId={} fields={}",
                userId,
                ex.getBindingResult().getFieldErrors().stream().map(f -> f.getField()).toList());

        // Tra AppError từ message của lỗi đầu tiên
        AppError appError = AppError.BATTLE_MODE_INVALID; // fallback
        var fieldErrors = ex.getBindingResult().getFieldErrors();
        if (!fieldErrors.isEmpty()) {
            String code = fieldErrors.get(0).getDefaultMessage();
            try {
                appError = AppError.valueOf(code);
            } catch (Exception ignored) {
            }
        }
        eventPublisher.sendError(userId, appError.getCode(), appError.getMessage());
    }

    // JSON không đọc được hoặc enum không hợp lệ
    @MessageExceptionHandler(MessageConversionException.class)
    public void handleConversion(MessageConversionException ex, Principal principal) {
        if (principal == null) return;
        Long userId = Long.parseLong(principal.getName());
        log.warn("Battle message conversion failed userId={} cause={}", userId, ex.getClass().getSimpleName());
        eventPublisher.sendError(userId,
                AppError.BATTLE_MODE_INVALID.getCode(),
                AppError.BATTLE_MODE_INVALID.getMessage());
    }

    // Exception bất ngờ — log.error với exception.
    @MessageExceptionHandler(Exception.class)
    public void handleGenericException(Exception ex, Principal principal) {
        log.error("Unexpected error in battle WebSocket handler", ex);
        if (principal == null) return;
        Long userId = Long.parseLong(principal.getName());
        eventPublisher.sendError(userId, "INTERNAL_ERROR", "An unexpected error occurred");
    }
}
