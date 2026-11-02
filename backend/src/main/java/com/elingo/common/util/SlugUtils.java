package com.elingo.common.util;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

public class SlugUtils {
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]+");

    private SlugUtils() {}

    public static String generateSlug(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "";
        }

        String slug = title.replace("đ", "d").replace("Đ", "D");
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        slug = DIACRITICS_PATTERN.matcher(slug).replaceAll("");
        slug = slug.toLowerCase();
        slug = NON_ALPHANUMERIC_PATTERN.matcher(slug).replaceAll("-");
        slug = slug.replaceAll("^-+|-+$", "");

        String randomStr = UUID.randomUUID().toString().substring(0, 8);
        return slug.isEmpty() ? randomStr : slug + "-" + randomStr;
    }
}
