package com.elingo.community.moderation.service;

import java.text.Normalizer;

/**
 * Bộ lọc tiếng Việt kiểm tra ký tự có dấu đặc trưng của tiếng Việt.
 * Hạn chế: tiếng Việt không dấu sẽ bị coi là ngôn ngữ khác và đi qua.
 */
public final class VietnameseDetector {
    private VietnameseDetector() { }

    private static final String VI_CHARS = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ";

    public static boolean containVietnamese(String text) {
        if (text == null) return false;
        String s = Normalizer.normalize(text, Normalizer.Form.NFC);
        for (int i = 0; i < s.length(); i++) {
            char c = Character.toLowerCase(s.charAt(i));
            if (Character.isLetter(c) && VI_CHARS.indexOf(c) >= 0) return true;
        }
        return false;
    }
}
