package com.elingo.battle.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class BattleTypingTest {

    // ========================================================================
    // normalize
    // ========================================================================
    @Nested
    @DisplayName("normalize")
    class NormalizeTests {

        @Test
        @DisplayName("Normalize returns an empty string for null")
        void normalize_Null() {
            assertThat(BattleTyping.normalize(null)).isEmpty();
        }

        @Test
        @DisplayName("Normalize returns an empty string for blank input")
        void normalize_Blank() {
            assertThat(BattleTyping.normalize("")).isEmpty();
            assertThat(BattleTyping.normalize("    ")).isEmpty();
        }

        @Test
        @DisplayName("Normalize lowercases the text")
        void normalize_Lowercase() {
            assertThat(BattleTyping.normalize("Ice CREAM")).isEqualTo("ice cream");
        }

        @Test
        @DisplayName("Normalize trims surrounding whitespace")
        void normalize_Trim() {
            assertThat(BattleTyping.normalize("  apple  ")).isEqualTo("apple");
        }

        @Test
        @DisplayName("Normalize collapses repeated whitespace into a single space")
        void normalize_CollapseWhitespace() {
            assertThat(BattleTyping.normalize("ice    cream")).isEqualTo("ice cream");
            assertThat(BattleTyping.normalize("ice\t\ncream")).isEqualTo("ice cream");
        }

        @Test
        @DisplayName("Normalize makes different spellings of whitespace and case comparable")
        void normalize_Comparable() {
            assertThat(BattleTyping.normalize("  Ice   CREAM ")).isEqualTo(BattleTyping.normalize("ice cream"));
        }

        @Test
        @DisplayName("Normalize keeps punctuation and digits")
        void normalize_KeepsPunctuation() {
            assertThat(BattleTyping.normalize("Well-Known 2")).isEqualTo("well-known 2");
        }
    }

    // ========================================================================
    // pattern
    // ========================================================================
    @Nested
    @DisplayName("pattern")
    class PatternTests {

        @Test
        @DisplayName("Pattern returns the length of each word")
        void pattern_MultipleWords() {
            assertThat(BattleTyping.pattern("ice cream")).containsExactly(3, 5);
            assertThat(BattleTyping.pattern("a big red apple")).containsExactly(1, 3, 3, 5);
        }

        @Test
        @DisplayName("Pattern returns a single length for a single word")
        void pattern_SingleWord() {
            assertThat(BattleTyping.pattern("apple")).containsExactly(5);
        }
    }

    // ========================================================================
    // firstChar
    // ========================================================================
    @Nested
    @DisplayName("firstChar")
    class FirstCharTests {

        @Test
        @DisplayName("First char returns the first character")
        void firstChar_Success() {
            assertThat(BattleTyping.firstChar("ice cream")).isEqualTo("i");
        }

        @Test
        @DisplayName("First char returns an empty string for an empty answer")
        void firstChar_Empty() {
            assertThat(BattleTyping.firstChar("")).isEmpty();
        }
    }

    @Test
    @DisplayName("Maximum answer length is 100 characters")
    void maxAnswerLength() {
        assertThat(BattleTyping.MAX_ANSWER_LENGTH).isEqualTo(100);
    }
}
