package com.elingo.community.moderation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class VietnameseDetectorTest {

    // ========================================================================
    // containVietnamese
    // ========================================================================
    @Nested
    @DisplayName("containVietnamese")
    class ContainVietnameseTests {

        @Test
        @DisplayName("Should return false when text is null")
        void containVietnamese_WhenNull_ReturnsFalse() {
            assertThat(VietnameseDetector.containVietnamese(null)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should return false when text is blank")
        void containVietnamese_WhenBlank_ReturnsFalse(String text) {
            assertThat(VietnameseDetector.containVietnamese(text)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Hello world",
                "This is a pure English sentence without diacritics",
                "1234567890",
                "@#$%^&*()_+",
                "Java Spring Boot REST API",
                "Good morning! Have a nice day."
        })
        @DisplayName("Should return false when text has no Vietnamese diacritics")
        void containVietnamese_WhenNoDiacritics_ReturnsFalse(String text) {
            assertThat(VietnameseDetector.containVietnamese(text)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Xin chào các bạn",
                "Hôm nay trời rất đẹp",
                "Tôi đang học lập trình",
                "Cảm ơn rất nhiều",
                "Chúc mừng năm mới",
                "Phở bò Hà Nội",
                "Bánh mì Sài Gòn"
        })
        @DisplayName("Should return true when text contains lowercase Vietnamese diacritics")
        void containVietnamese_WhenLowercaseDiacritics_ReturnsTrue(String text) {
            assertThat(VietnameseDetector.containVietnamese(text)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM",
                "ĐỘC LẬP TỰ DO HẠNH PHÚC",
                "ĐÀ NẴNG",
                "HẢI PHÒNG",
                "HÀ NỘI"
        })
        @DisplayName("Should return true when text contains uppercase Vietnamese diacritics")
        void containVietnamese_WhenUppercaseDiacritics_ReturnsTrue(String text) {
            assertThat(VietnameseDetector.containVietnamese(text)).isTrue();
        }

        @Test
        @DisplayName("Should return true for individual special Vietnamese letters like d with stroke")
        void containVietnamese_WhenSpecialLetterD_ReturnsTrue() {
            assertThat(VietnameseDetector.containVietnamese("đường")).isTrue();
            assertThat(VietnameseDetector.containVietnamese("Đường")).isTrue();
        }

        @Test
        @DisplayName("Should return true when Vietnamese diacritics are mixed with English and numbers")
        void containVietnamese_WhenMixedText_ReturnsTrue() {
            assertThat(VietnameseDetector.containVietnamese("Spring Boot rất mạnh mẽ! Version 3.4.0")).isTrue();
        }
    }
}
