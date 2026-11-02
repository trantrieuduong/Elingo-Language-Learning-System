package com.elingo.common.util;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

public final class CursorUtils {

    private CursorUtils() {
    }

    public record CursorData(LocalDateTime createdAt, Long id) {
    }

    /**
     * Encode LocalDateTime and id into URL-safe Base64 cursor string.
     */
    public static String encode(LocalDateTime createdAt, Long id) {
        if (createdAt == null || id == null) {
            return null;
        }
        String raw = createdAt + "_" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode URL-safe Base64 cursor string into CursorData (createdAt and id).
     * Returns null if cursor is null, blank, or invalid.
     */
    public static CursorData decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(cursor.trim());
            String raw = new String(decodedBytes, StandardCharsets.UTF_8);
            int lastUnderscore = raw.lastIndexOf('_');
            if (lastUnderscore <= 0 || lastUnderscore >= raw.length() - 1) {
                return null;
            }
            LocalDateTime createdAt = LocalDateTime.parse(raw.substring(0, lastUnderscore));
            Long id = Long.parseLong(raw.substring(lastUnderscore + 1));
            return new CursorData(createdAt, id);
        } catch (Exception e) {
            return null;
        }
    }
}

