package com.elingo.learning.lesson.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class TranscriptUtilsTest {

    // ========================================================================
    // countWords
    // ========================================================================
    @Nested
    @DisplayName("countWords")
    class CountWordsTests {

        @Test
        @DisplayName("Count words returns zero for null")
        void countWords_Null() {
            assertThat(TranscriptUtils.countWords(null)).isZero();
        }

        @Test
        @DisplayName("Count words returns zero for empty and blank transcripts")
        void countWords_Blank() {
            assertThat(TranscriptUtils.countWords("")).isZero();
            assertThat(TranscriptUtils.countWords("    ")).isZero();
            assertThat(TranscriptUtils.countWords(" \t\n ")).isZero();
        }

        @Test
        @DisplayName("Count words returns one for a single word")
        void countWords_SingleWord() {
            assertThat(TranscriptUtils.countWords("hello")).isEqualTo(1);
        }

        @Test
        @DisplayName("Count words counts words separated by single spaces")
        void countWords_MultipleWords() {
            assertThat(TranscriptUtils.countWords("hello big wide world")).isEqualTo(4);
        }

        @Test
        @DisplayName("Count words ignores leading and trailing whitespace")
        void countWords_LeadingAndTrailingWhitespace() {
            assertThat(TranscriptUtils.countWords("   hello world   ")).isEqualTo(2);
        }

        @Test
        @DisplayName("Count words treats repeated whitespace, tabs and newlines as one separator")
        void countWords_RepeatedWhitespace() {
            assertThat(TranscriptUtils.countWords("hello    world")).isEqualTo(2);
            assertThat(TranscriptUtils.countWords("hello\tbig\nworld")).isEqualTo(3);
        }

        @Test
        @DisplayName("Count words counts punctuation attached to a word as part of the word")
        void countWords_Punctuation() {
            assertThat(TranscriptUtils.countWords("hello, world!")).isEqualTo(2);
        }
    }

    // ========================================================================
    // normalize
    // ========================================================================
    @Nested
    @DisplayName("normalize")
    class NormalizeTests {

        @Test
        @DisplayName("should return empty string when input is null")
        void normalize_Null() {
            assertThat(TranscriptUtils.normalize(null)).isEmpty();
        }

        @Test
        @DisplayName("should return empty string when input is blank or contains only punctuation")
        void normalize_BlankOrOnlyPunctuation() {
            assertThat(TranscriptUtils.normalize("")).isEmpty();
            assertThat(TranscriptUtils.normalize("   \t\n  ")).isEmpty();
            assertThat(TranscriptUtils.normalize("!!! — ... ???")).isEmpty();
        }

        @Test
        @DisplayName("should lowercase, strip punctuation, and collapse whitespace")
        void normalize_PunctuationAndSpaces() {
            assertThat(TranscriptUtils.normalize("  Hi, can I get   a large coffee, please?  "))
                    .isEqualTo("hi can i get a large coffee please");
        }

        @Test
        @DisplayName("should strip straight and curly apostrophes to match frontend cleanWord")
        void normalize_Apostrophes() {
            assertThat(TranscriptUtils.normalize("Don't worry — it\u2019s \u2018fine\u2019!"))
                    .isEqualTo("dont worry its fine");
        }

        @Test
        @DisplayName("should preserve unicode letters and digits")
        void normalize_UnicodeAndDigits() {
            assertThat(TranscriptUtils.normalize("Café 101!"))
                    .isEqualTo("café 101");
        }
    }
}
