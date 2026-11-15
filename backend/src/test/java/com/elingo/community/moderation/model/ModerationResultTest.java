package com.elingo.community.moderation.model;

import com.elingo.community.entity.ModerationAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class ModerationResultTest {

    // ========================================================================
    // clean
    // ========================================================================
    @Nested
    @DisplayName("clean")
    class CleanTests {

        @Test
        @DisplayName("clean() should return AUTO_APPROVE result with clean label and 0.0 score")
        void clean_ReturnsDefaultCleanResult() {
            ModerationResult clean = ModerationResult.clean();

            assertThat(clean.action()).isEqualTo(ModerationAction.AUTO_APPROVE);
            assertThat(clean.label()).isEqualTo("clean");
            assertThat(clean.score()).isEqualTo(0.0);
            assertThat(clean.note()).isNull();
        }
    }

    // ========================================================================
    // worseOf
    // ========================================================================
    @Nested
    @DisplayName("worseOf")
    class WorseOfTests {

        @Test
        @DisplayName("worseOf should return result with higher severity action")
        void worseOf_DifferentSeverity_ReturnsHigherSeverity() {
            ModerationResult clean = ModerationResult.clean();
            ModerationResult review = new ModerationResult(ModerationAction.FLAG_FOR_REVIEW, "toxic", 0.6, null);
            ModerationResult reject = new ModerationResult(ModerationAction.AUTO_REJECT, "hate", 0.95, null);

            // AUTO_APPROVE vs FLAG_FOR_REVIEW
            assertThat(clean.worseOf(review)).isEqualTo(review);
            assertThat(review.worseOf(clean)).isEqualTo(review);

            // FLAG_FOR_REVIEW vs AUTO_REJECT
            assertThat(review.worseOf(reject)).isEqualTo(reject);
            assertThat(reject.worseOf(review)).isEqualTo(reject);

            // AUTO_APPROVE vs AUTO_REJECT
            assertThat(clean.worseOf(reject)).isEqualTo(reject);
            assertThat(reject.worseOf(clean)).isEqualTo(reject);
        }

        @Test
        @DisplayName("worseOf should return result with higher score when severity is identical")
        void worseOf_SameSeverity_ReturnsHigherScore() {
            ModerationResult r1 = new ModerationResult(ModerationAction.FLAG_FOR_REVIEW, "spam", 0.55, null);
            ModerationResult r2 = new ModerationResult(ModerationAction.FLAG_FOR_REVIEW, "hate", 0.65, null);

            assertThat(r1.worseOf(r2)).isEqualTo(r2);
            assertThat(r2.worseOf(r1)).isEqualTo(r2);

            // When scores are equal, returns this
            ModerationResult r3 = new ModerationResult(ModerationAction.FLAG_FOR_REVIEW, "toxic", 0.55, null);
            assertThat(r1.worseOf(r3)).isEqualTo(r1);
        }

        @Test
        @DisplayName("worseOf should throw IllegalArgumentException when comparing manual moderation actions")
        void worseOf_WithManualAction_ThrowsException() {
            ModerationResult clean = ModerationResult.clean();
            ModerationResult manualApprove = new ModerationResult(ModerationAction.MANUAL_APPROVE, "clean", 0.0, null);
            ModerationResult manualReject = new ModerationResult(ModerationAction.MANUAL_REJECT, "spam", 1.0, null);

            assertThatThrownBy(() -> clean.worseOf(manualApprove))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Manual actions are not produced by moderation");

            assertThatThrownBy(() -> manualReject.worseOf(clean))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Manual actions are not produced by moderation");
        }
    }
}
