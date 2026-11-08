package com.elingo.community.moderation.stage;

import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.model.PostForModeration;
import com.elingo.community.moderation.moderator.TextModerator;
import com.elingo.community.moderation.stage.impl.TextStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class TextStageTest {

    @Mock
    private TextModerator moderator1;

    @Mock
    private TextModerator moderator2;

    // ========================================================================
    // name
    // ========================================================================
    @Nested
    @DisplayName("name")
    class NameTests {

        @Test
        @DisplayName("Should return TEXT")
        void name_ReturnsText() {
            TextStage stage = new TextStage(List.of(moderator1));
            assertThat(stage.name()).isEqualTo("TEXT");
        }
    }

    // ========================================================================
    // evaluate
    // ========================================================================
    @Nested
    @DisplayName("evaluate")
    class EvaluateTests {

        @Test
        @DisplayName("Should return Optional.empty when title and content are blank")
        void evaluate_WhenTitleAndContentBlank_ReturnsEmpty() {
            TextStage stage = new TextStage(List.of(moderator1));
            PostForModeration post = new PostForModeration(1L, 10L, "", "   ", List.of());

            Optional<ModerationResult> result = stage.evaluate(post);

            assertThat(result).isEmpty();
            verifyNoInteractions(moderator1);
        }

        @Test
        @DisplayName("Should find first supporting moderator and moderate text")
        void evaluate_WhenModeratorSupports_CallsFirstSupportingModerator() {
            TextStage stage = new TextStage(List.of(moderator1, moderator2));
            PostForModeration post = new PostForModeration(1L, 10L, "Tiêu đề", "Nội dung bài viết", List.of());
            String expectedText = "Tiêu đề Nội dung bài viết";
            ModerationResult cleanResult = ModerationResult.clean();

            when(moderator1.supports(expectedText)).thenReturn(false);
            when(moderator2.supports(expectedText)).thenReturn(true);
            when(moderator2.moderateText(expectedText)).thenReturn(cleanResult);

            Optional<ModerationResult> result = stage.evaluate(post);

            assertThat(result).contains(cleanResult);
            verify(moderator1).supports(expectedText);
            verify(moderator1, never()).moderateText(anyString());
            verify(moderator2).supports(expectedText);
            verify(moderator2).moderateText(expectedText);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when no moderator supports the text")
        void evaluate_WhenNoModeratorSupports_ThrowsIllegalStateException() {
            TextStage stage = new TextStage(List.of(moderator1));
            PostForModeration post = new PostForModeration(1L, 10L, "Sample", "Text", List.of());
            String text = "Sample Text";

            when(moderator1.supports(text)).thenReturn(false);

            assertThatThrownBy(() -> stage.evaluate(post))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("None of TextModerator support this text");
        }
    }
}
