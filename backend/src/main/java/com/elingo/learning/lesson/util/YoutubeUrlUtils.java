package com.elingo.learning.lesson.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YoutubeUrlUtils {
    private static final Pattern YOUTUBE_URL = Pattern.compile(
            "^(?:https?://)?(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{11})(?:[&?#/].*)?$");

    private YoutubeUrlUtils() {}

    // trả video id 11 ký tự, hoặc empty nếu không phải link YouTube hợp lệ
    public static Optional<String> extractVideoId(String url) {
        if (url == null) return Optional.empty();
        Matcher m = YOUTUBE_URL.matcher(url.trim());
        return m.matches() ? Optional.of(m.group(1)) : Optional.empty();
    }

    public static boolean isValid(String url) {
        return extractVideoId(url).isPresent();
    }
}
