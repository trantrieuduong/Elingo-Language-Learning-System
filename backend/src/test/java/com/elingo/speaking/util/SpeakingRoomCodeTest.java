package com.elingo.speaking.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class SpeakingRoomCodeTest {

    // ========================================================================
    // generate
    // ========================================================================
    @Nested
    @DisplayName("generate")
    class GenerateTests {

        @Test
        @DisplayName("Generate returns a code of 6 characters")
        void generate_Length() {
            assertThat(SpeakingRoomCode.generate()).hasSize(6);
        }

        @Test
        @DisplayName("Generate only uses unambiguous uppercase letters and digits")
        void generate_Alphabet() {
            for (int i = 0; i < 500; i++) {
                // Không dùng 0, 1, I, O để tránh nhầm lẫn khi nhập tay
                assertThat(SpeakingRoomCode.generate()).matches("[A-HJ-NP-Z2-9]{6}");
            }
        }

        @Test
        @DisplayName("Generate returns different codes")
        void generate_Random() {
            Set<String> codes = new HashSet<>();
            for (int i = 0; i < 200; i++) {
                codes.add(SpeakingRoomCode.generate());
            }

            assertThat(codes.size()).isGreaterThan(190);
        }

        @Test
        @DisplayName("Generated codes are already normalized")
        void generate_AlreadyNormalized() {
            String code = SpeakingRoomCode.generate();

            assertThat(SpeakingRoomCode.normalize(code)).isEqualTo(code);
        }
    }

    // ========================================================================
    // normalize
    // ========================================================================
    @Nested
    @DisplayName("normalize")
    class NormalizeTests {

        @ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
        @CsvSource({
                "abc234, ABC234",
                "ABC234, ABC234",
                "'  abc234  ', ABC234",
                "aBc234, ABC234"
        })
        @DisplayName("Normalize trims and uppercases the code")
        void normalize_TrimAndUppercase(String raw, String expected) {
            assertThat(SpeakingRoomCode.normalize(raw)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Normalize returns an empty string for null")
        void normalize_Null() {
            assertThat(SpeakingRoomCode.normalize(null)).isEmpty();
        }

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @CsvSource({"''", "'   '"})
        @DisplayName("Normalize returns an empty string for blank input")
        void normalize_Blank(String raw) {
            assertThat(SpeakingRoomCode.normalize(raw)).isEmpty();
        }

        @Test
        @DisplayName("Normalize does not depend on the default locale")
        void normalize_LocaleIndependent() {
            Locale original = Locale.getDefault();
            try {
                // Locale tiếng Thổ Nhĩ Kỳ biến "i" thành "İ" nếu không dùng Locale.ROOT
                Locale.setDefault(Locale.forLanguageTag("tr-TR"));

                assertThat(SpeakingRoomCode.normalize("i")).isEqualTo("I");
            } finally {
                Locale.setDefault(original);
            }
        }
    }
}
