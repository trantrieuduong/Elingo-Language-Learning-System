package com.elingo.community.moderation.stage;

import com.elingo.common.enums.MediaKind;
import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.model.PostForModeration;
import com.elingo.community.moderation.model.PostForModeration.PostMediaItem;
import com.elingo.community.moderation.moderator.ImageModerator;
import com.elingo.community.moderation.stage.impl.ImageStage;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ImageStageTest {

    @Mock
    private ImageModerator imageModerator;

    // ========================================================================
    // name
    // ========================================================================
    @Nested
    @DisplayName("name")
    class NameTests {

        @Test
        @DisplayName("Should return IMAGE")
        void name_ReturnsImage() {
            ImageStage stage = new ImageStage(imageModerator);
            assertThat(stage.name()).isEqualTo("IMAGE");
        }
    }

    // ========================================================================
    // evaluate
    // ========================================================================
    @Nested
    @DisplayName("evaluate")
    class EvaluateTests {

        @Test
        @DisplayName("Should return Optional.empty when post has no IMAGE media")
        void evaluate_WhenNoImageMedia_ReturnsEmpty() {
            ImageStage stage = new ImageStage(imageModerator);
            PostForModeration post = new PostForModeration(1L, 10L, "Title", "Content", List.of(
                    new PostMediaItem(MediaKind.VIDEO, "https://r2.elingo.pro/video.mp4")
            ));

            Optional<ModerationResult> result = stage.evaluate(post);

            assertThat(result).isEmpty();
            verifyNoInteractions(imageModerator);
        }

        @Test
        @DisplayName("Should return AUTO_REJECT immediately when first image is rejected")
        void evaluate_WhenFirstImageRejected_ShortCircuits() {
            ImageStage stage = new ImageStage(imageModerator);
            PostForModeration post = new PostForModeration(1L, 10L, "Title", "Content", List.of(
                    new PostMediaItem(MediaKind.IMAGE, "https://r2.elingo.pro/img1.jpg"),
                    new PostMediaItem(MediaKind.IMAGE, "https://r2.elingo.pro/img2.jpg")
            ));

            ModerationResult rejectResult = new ModerationResult(ModerationAction.AUTO_REJECT, "violence", 0.95, null);
            when(imageModerator.moderateImage("https://r2.elingo.pro/img1.jpg")).thenReturn(rejectResult);

            Optional<ModerationResult> result = stage.evaluate(post);

            assertThat(result).contains(rejectResult);
            verify(imageModerator, times(1)).moderateImage(anyString());
            verify(imageModerator, never()).moderateImage("https://r2.elingo.pro/img2.jpg");
        }

        @Test
        @DisplayName("Should evaluate all images and return the worst result")
        void evaluate_WhenMultipleImages_ReturnsWorstResult() {
            ImageStage stage = new ImageStage(imageModerator);
            PostForModeration post = new PostForModeration(1L, 10L, "Title", "Content", List.of(
                    new PostMediaItem(MediaKind.IMAGE, "https://r2.elingo.pro/img1.jpg"),
                    new PostMediaItem(MediaKind.IMAGE, "https://r2.elingo.pro/img2.jpg")
            ));

            ModerationResult cleanResult = ModerationResult.clean();
            ModerationResult flagResult = new ModerationResult(ModerationAction.FLAG_FOR_REVIEW, "sexual", 0.60, null);

            when(imageModerator.moderateImage("https://r2.elingo.pro/img1.jpg")).thenReturn(cleanResult);
            when(imageModerator.moderateImage("https://r2.elingo.pro/img2.jpg")).thenReturn(flagResult);

            Optional<ModerationResult> result = stage.evaluate(post);

            assertThat(result).contains(flagResult);
            verify(imageModerator).moderateImage("https://r2.elingo.pro/img1.jpg");
            verify(imageModerator).moderateImage("https://r2.elingo.pro/img2.jpg");
        }

        @Test
        @DisplayName("Should return clean when all images are clean")
        void evaluate_WhenAllImagesClean_ReturnsClean() {
            ImageStage stage = new ImageStage(imageModerator);
            PostForModeration post = new PostForModeration(1L, 10L, "Title", "Content", List.of(
                    new PostMediaItem(MediaKind.IMAGE, "https://r2.elingo.pro/img1.jpg")
            ));

            when(imageModerator.moderateImage("https://r2.elingo.pro/img1.jpg")).thenReturn(ModerationResult.clean());

            Optional<ModerationResult> result = stage.evaluate(post);

            assertThat(result).isPresent();
            assertThat(result.get().action()).isEqualTo(ModerationAction.AUTO_APPROVE);
        }
    }
}
