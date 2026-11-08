package com.elingo.community.moderation.exception;

/** Lỗi hạ tầng/tạm thời (service down, timeout, 429, 5xx). Không phải lỗi của bài viết: scheduler tạm dừng rồi thử lại. */
public class ModerationUnavailableException extends RuntimeException {
    public ModerationUnavailableException(String message) { super(message); }
    public ModerationUnavailableException(String message, Throwable cause) { super(message, cause); }
}
