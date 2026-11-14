package com.elingo.common.exception;

import com.elingo.common.dto.WsErrorPayload;
import com.elingo.common.websocket.WsErrorSender;
import com.elingo.common.dto.WsEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.security.Principal;
import java.util.List;

@Slf4j(topic = "WS-EXCEPTION-HANDLER")
@ControllerAdvice
@RequiredArgsConstructor
public class WsExceptionHandler {

    private final List<WsErrorSender> senders;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageExceptionHandler(AppException.class)
    public void handleAppException(AppException ex, Principal principal, SimpMessageHeaderAccessor headers) {
        AppError e = ex.getAppError();
        log.warn("WS business error code={}", e.getCode());
        reply(principal, headers, e.getCode(), e.getMessage());
    }

    // @Valid trên payload STOMP thất bại
    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidation(MethodArgumentNotValidException ex, Principal principal, SimpMessageHeaderAccessor headers) {
        var fieldErrors = ex.getBindingResult().getFieldErrors();
        log.warn("WS payload validation failed fields={}", fieldErrors.stream().map(FieldError::getField).toList());
        AppError e = AppError.INVALID_REQUEST; // mặc định chung
        if (!fieldErrors.isEmpty()) {
            try { e = AppError.valueOf(fieldErrors.getFirst().getDefaultMessage()); } catch (Exception ignored) {}   // quy ước repo: message validate = tên AppError
        }
        reply(principal, headers, e.getCode(), e.getMessage());
    }

    // JSON hỏng / enum sai giá trị
    @MessageExceptionHandler(MessageConversionException.class)
    public void handleConversion(MessageConversionException ex, Principal principal, SimpMessageHeaderAccessor headers) {
        log.warn("WS message conversion failed cause={}", ex.getClass().getSimpleName());
        reply(principal, headers, AppError.MALFORMED_JSON.getCode(), AppError.MALFORMED_JSON.getMessage());
    }

    // Không đủ quyền truy cập
    @MessageExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex, Principal principal, SimpMessageHeaderAccessor headers) {
        log.warn("WS access denied: {}", ex.getMessage());
        reply(principal, headers, AppError.UNAUTHORIZED.getCode(), AppError.UNAUTHORIZED.getMessage());
    }

    // Lỗi bất ngờ: trả mã chung
    @MessageExceptionHandler(Exception.class)
    public void handleGeneric(Exception ex, Principal principal, SimpMessageHeaderAccessor headers) {
        log.error("Unexpected error in WebSocket handler", ex);
        reply(principal, headers, AppError.INTERNAL_SERVER_ERROR.getCode(), AppError.INTERNAL_SERVER_ERROR.getMessage());
    }

    private void reply(Principal principal, SimpMessageHeaderAccessor headers, String code, String message) {
        if (principal == null) return;
        String destination = headers.getDestination();
        senders.stream()
                .filter(s -> destination != null && s.destinationPrefix() != null && destination.startsWith(s.destinationPrefix()))
                .findFirst()
                .ifPresentOrElse(
                        s -> s.sendError(Long.parseLong(principal.getName()), code, message),
                        () -> {
                            log.warn("No WsErrorSender for destination={}, fallback to /queue/errors", destination);
                            messagingTemplate.convertAndSendToUser(
                                    principal.getName(),
                                    "/queue/errors",
                                    new WsEventEnvelope<>("error", new WsErrorPayload(code, message))
                            );
                        });
    }
}

