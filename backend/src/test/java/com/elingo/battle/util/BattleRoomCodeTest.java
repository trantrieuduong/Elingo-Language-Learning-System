package com.elingo.battle.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class BattleRoomCodeTest {

    // ========================================================================
    // generate
    // ========================================================================
    @Nested
    @DisplayName("generate")
    class GenerateTests {

        @Test
        @DisplayName("Generate returns a code of 6 characters")
        void generate_Length() {
            assertThat(BattleRoomCode.generate()).hasSize(6);
        }

        @Test
        @DisplayName("Generate only uses unambiguous uppercase letters and digits")
        void generate_Alphabet() {
            for (int i = 0; i < 500; i++) {
                // Không dùng 0, 1, I, O để tránh nhầm lẫn khi nhập tay
                assertThat(BattleRoomCode.generate()).matches("[A-HJ-NP-Z2-9]{6}");
            }
        }

        @Test
        @DisplayName("Generate returns different codes")
        void generate_Random() {
            Set<String> codes = new HashSet<>();
            for (int i = 0; i < 200; i++) {
                codes.add(BattleRoomCode.generate());
            }

            assertThat(codes.size()).isGreaterThan(190);
        }
    }

    // ========================================================================
    // normalize
    // ========================================================================
    @Nested
    @DisplayName("normalize")
    class NormalizeTests {

        @Test
        @DisplayName("Normalize returns an empty string for null")
        void normalize_Null() {
            assertThat(BattleRoomCode.normalize(null)).isEmpty();
        }

        @Test
        @DisplayName("Normalize trims and uppercases the code")
        void normalize_Success() {
            assertThat(BattleRoomCode.normalize("  abc234 ")).isEqualTo("ABC234");
        }

        @Test
        @DisplayName("Normalize keeps an already normalized code unchanged")
        void normalize_AlreadyNormalized() {
            assertThat(BattleRoomCode.normalize("ABC234")).isEqualTo("ABC234");
        }

        @Test
        @DisplayName("Normalize returns an empty string for blank input")
        void normalize_Blank() {
            assertThat(BattleRoomCode.normalize("   ")).isEmpty();
        }

        @Test
        @DisplayName("Normalize makes generated codes round-trip")
        void normalize_GeneratedCode() {
            String code = BattleRoomCode.generate();

            assertThat(BattleRoomCode.normalize(code.toLowerCase())).isEqualTo(code);
        }
    }
}
