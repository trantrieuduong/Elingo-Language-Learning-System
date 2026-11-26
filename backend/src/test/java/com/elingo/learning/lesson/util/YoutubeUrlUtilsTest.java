package com.elingo.learning.lesson.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("YoutubeUrlUtils")
class YoutubeUrlUtilsTest {

    private static final String VIDEO_ID = "dQw4w9WgXcQ";

    @Nested
    @DisplayName("extractVideoId")
    class ExtractVideoId {

        @ParameterizedTest
        @ValueSource(strings = {
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "http://youtube.com/watch?v=dQw4w9WgXcQ",
                "https://m.youtube.com/watch?v=dQw4w9WgXcQ&t=42s",
                "https://www.youtube.com/watch?feature=share&v=dQw4w9WgXcQ",
                "youtube.com/watch?v=dQw4w9WgXcQ",
                "https://youtu.be/dQw4w9WgXcQ",
                "https://youtu.be/dQw4w9WgXcQ?si=abc123",
                "https://www.youtube.com/embed/dQw4w9WgXcQ",
                "https://www.youtube.com/shorts/dQw4w9WgXcQ",
                "  https://www.youtube.com/watch?v=dQw4w9WgXcQ  "
        })
        @DisplayName("should extract 11-character video id for valid YouTube URLs")
        void shouldExtractVideoId_whenUrlIsValid(String url) {
            assertThat(YoutubeUrlUtils.extractVideoId(url)).contains(VIDEO_ID);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                "   ",
                "https://vimeo.com/123456789",
                "https://www.youtube.com/watch",
                "https://www.youtube.com/watch?v=short",
                "https://www.youtube.com/watch?v=tooLongVideoId12",
                "https://www.youtube.com/watch?v=invalid!@#$",
                "https://notyoutube.com/watch?v=dQw4w9WgXcQ",
                "https://www.youtube.com/channel/UCuAXFkgsw1L7xaCfnd5JJOw"
        })
        @DisplayName("should return empty when URL is null, blank, or not a valid YouTube video URL")
        void shouldReturnEmpty_whenUrlIsInvalid(String url) {
            assertThat(YoutubeUrlUtils.extractVideoId(url)).isEmpty();
        }
    }

    @Nested
    @DisplayName("isValid")
    class IsValid {

        @Test
        @DisplayName("should return true for valid YouTube URL")
        void shouldReturnTrue_whenUrlIsValid() {
            assertThat(YoutubeUrlUtils.isValid("https://www.youtube.com/watch?v=dQw4w9WgXcQ")).isTrue();
        }

        @Test
        @DisplayName("should return false for invalid or null URL")
        void shouldReturnFalse_whenUrlIsInvalid() {
            assertThat(YoutubeUrlUtils.isValid(null)).isFalse();
            assertThat(YoutubeUrlUtils.isValid("https://example.com/video")).isFalse();
        }
    }
}
