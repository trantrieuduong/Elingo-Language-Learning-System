package com.elingo.learning.lesson.util;

/**
 * Đếm số từ trong transcript đã chuẩn hóa (tách theo khoảng trắng); trả 0 nếu null hoặc blank.
 */
public final class TranscriptUtils {

    private TranscriptUtils() {
    }

    public static int countWords(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return 0;
        }
        return transcript.trim().split("\\s+").length;
    }
}
