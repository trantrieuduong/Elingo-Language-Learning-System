package com.elingo.battle.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BattleTyping {

    public static final int MAX_ANSWER_LENGTH = 100;

    private BattleTyping() {}

    // Chuẩn hóa chuỗi
    public static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    // Lấy độ dài từng từ
    public static List<Integer> pattern(String normalizedAnswer) {
        return Arrays.stream(normalizedAnswer.split(" ")).map(String::length).toList();
    }

    // Lấy ký tự đầu tiên
    public static String firstChar(String normalizedAnswer) {
        return normalizedAnswer.isEmpty() ? "" : normalizedAnswer.substring(0, 1);
    }
}

