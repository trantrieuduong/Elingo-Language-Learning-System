package com.elingo.learning.lesson.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tiện ích xử lý transcript cho bài học
 */
public final class TranscriptUtils {

    // Giữ lại chữ cái Unicode, chữ số; bỏ mọi thứ còn lại (kể cả dấu nháy đơn)
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final Pattern SPACES   = Pattern.compile("\\s+");

    private TranscriptUtils() {
    }

    public static int countWords(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return 0;
        }
        return transcript.trim().split("\\s+").length;
    }

    /**
     * Chuẩn hóa transcript: chữ thường, bỏ ký tự không phải chữ/số (kể cả dấu nháy đơn), gộp khoảng trắng, trim
     */
    public static String normalize(String original) {
        if (original == null) return "";
        String s = original.toLowerCase(Locale.ROOT)
                           .replace('\u2018', '\'')   // '
                           .replace('\u2019', '\'');  // '
        s = NON_WORD.matcher(s).replaceAll("");
        return SPACES.matcher(s).replaceAll(" ").trim();
    }
}
