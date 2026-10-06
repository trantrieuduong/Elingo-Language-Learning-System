package com.elingo.file.dto.response;

import java.time.LocalDateTime;

/**
 * @param url  URL có chữ ký để client PUT thẳng lên R2
 * @param key  key tạm trong {@code staging/}, dùng cho bước verify
 */
public record PresignedUrlResponse(String url, String key, LocalDateTime expiresAt) {
}
