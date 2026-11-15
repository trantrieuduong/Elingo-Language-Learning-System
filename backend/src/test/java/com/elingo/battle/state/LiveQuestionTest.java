package com.elingo.battle.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class LiveQuestionTest {

    private LiveQuestion multipleChoice() {
        return new LiveQuestion(1001L, 1, "apple", "tao", 15000, List.of(
                new LiveQuestion.LiveOption(1L, "tao", true),
                new LiveQuestion.LiveOption(2L, "cam", false),
                new LiveQuestion.LiveOption(3L, "chuoi", false),
                new LiveQuestion.LiveOption(4L, "nho", false)));
    }

    // ========================================================================
    // isTyping
    // ========================================================================
    @Nested
    @DisplayName("isTyping")
    class IsTypingTests {

        @Test
        @DisplayName("Question without options is a typing question")
        void isTyping_EmptyOptions() {
            assertThat(new LiveQuestion(1L, 1, "kem", "ice cream", 15000, List.of()).isTyping()).isTrue();
        }

        @Test
        @DisplayName("Question with null options is a typing question")
        void isTyping_NullOptions() {
            assertThat(new LiveQuestion(1L, 1, "kem", "ice cream", 15000, null).isTyping()).isTrue();
        }

        @Test
        @DisplayName("Question with options is not a typing question")
        void isTyping_WithOptions() {
            assertThat(multipleChoice().isTyping()).isFalse();
        }
    }

    // ========================================================================
    // hasOption
    // ========================================================================
    @Nested
    @DisplayName("hasOption")
    class HasOptionTests {

        @Test
        @DisplayName("hasOption is true for every option of the question")
        void hasOption_Success() {
            LiveQuestion question = multipleChoice();

            assertThat(question.hasOption(1L)).isTrue();
            assertThat(question.hasOption(4L)).isTrue();
        }

        @Test
        @DisplayName("hasOption is false for an option of another question")
        void hasOption_UnknownOption() {
            assertThat(multipleChoice().hasOption(999L)).isFalse();
        }
    }

    // ========================================================================
    // isCorrectOption
    // ========================================================================
    @Nested
    @DisplayName("isCorrectOption")
    class IsCorrectOptionTests {

        @Test
        @DisplayName("isCorrectOption is true only for the correct option")
        void isCorrectOption_Success() {
            LiveQuestion question = multipleChoice();

            assertThat(question.isCorrectOption(1L)).isTrue();
            assertThat(question.isCorrectOption(2L)).isFalse();
            assertThat(question.isCorrectOption(4L)).isFalse();
        }

        @Test
        @DisplayName("isCorrectOption is false for an unknown option")
        void isCorrectOption_UnknownOption() {
            assertThat(multipleChoice().isCorrectOption(999L)).isFalse();
        }
    }
}
