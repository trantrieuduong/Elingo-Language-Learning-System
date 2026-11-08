package com.elingo.community.moderation.exception;

/** Nội dung không kiểm duyệt tự động được (video hỏng, ảnh quá lớn, 4xx...). Chuyển thẳng cho admin duyệt tay. */
public class UnprocessableContentException extends RuntimeException {
    public UnprocessableContentException(String message) { super(message); }
    public UnprocessableContentException(String message, Throwable cause) { super(message, cause); }
}
