package com.elingo.community.moderation.stage;

import com.elingo.common.enums.MediaKind;
import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.model.FrameBatch;
import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.model.PostForModeration;
import com.elingo.community.moderation.model.PostForModeration.PostMediaItem;
import com.elingo.community.moderation.moderator.ImageModerator;
import com.elingo.community.moderation.service.FfmpegFrameExtractor;
import com.elingo.community.moderation.stage.impl.VideoStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class VideoStageTest {

    @Mock
    private FfmpegFrameExtractor extractor;

    @Mock
    private ImageModerator imageModerator;

    // ========================================================================
    // name
    // ========================================================================
    @Nested
    @DisplayName("name")
    class NameTests {

        @Test
        @DisplayName("Should return VIDEO")
        void name_ReturnsVideo() {
            VideoStage stage = new VideoStage(extractor, imageModerator);
            assertThat(stage.name()).isEqualTo("VIDEO");
        }
    }

    // ========================================================================
    // evaluate
    // ========================================================================
    @Nested
    @DisplayName("evaluate")
    class EvaluateTests {

        @Test
        @DisplayName("Should return Optional.empty when post has no VIDEO media")
        void evaluate_WhenNoVideoMedia_ReturnsEmpty() {
            VideoStage stage = new VideoStage(extractor, imageModerator);
            PostForModeration post = new PostForModeration(1L, 10L, "Title", "Content", List.of(
                    new PostMediaItem(MediaKind.IMAGE, "https://r2.elingo.pro/image.png")
            ));

            Optional<ModerationResult> result = stage.evaluate(post);

            assertThat(result).isEmpty();
            verifyNoInteractions(extractor);
            verifyNoInteractions(imageModerator);
        }

        @Test
        @DisplayName("Should short-circuit and return AUTO_REJECT when a frame is rejected")
        void evaluate_WhenFrameRejected_ShortCircuits(@TempDir Path tempDir) throws IOException {
            VideoStage stage = new VideoStage(extractor, imageModerator);
            String videoUrl = "https://r2.elingo.pro/clip.mp4";
            PostForModeration post = new PostForModeration(1L, 10L, "Title", "Content", List.of(
                    new PostMediaItem(MediaKind.VIDEO, videoUrl)
            ));

            Path frame1 = Files.write(tempDir.resolve("frame1.jpg"), new byte[]{1, 2, 3});
            Path frame2 = Files.write(tempDir.resolve("frame2.jpg"), new byte[]{4, 5, 6});
            FrameBatch batch = new FrameBatch(tempDir, List.of(frame1, frame2));

            when(extractor.extract(videoUrl)).thenReturn(batch);

            ModerationResult rejectResult = new ModerationResult(ModerationAction.AUTO_REJECT, "violence", 0.96, null);
            when(imageModerator.moderateImage(eq("image/jpeg"), any(byte[].class)))
                    .thenReturn(rejectResult);

            Optional<ModerationResult> result = stage.evaluate(post);

            assertThat(result).contains(rejectResult);
            verify(imageModerator, times(1)).moderateImage(eq("image/jpeg"), any(byte[].class));
        }

        @Test
        @DisplayName("Should moderate all frames and return the worst result")
        void evaluate_WhenMultipleFrames_ReturnsWorstResult(@TempDir Path tempDir) throws IOException {
            VideoStage stage = new VideoStage(extractor, imageModerator);
            String videoUrl = "https://r2.elingo.pro/clip.mp4";
            PostForModeration post = new PostForModeration(1L, 10L, "Title", "Content", List.of(
                    new PostMediaItem(MediaKind.VIDEO, videoUrl)
            ));

            Path frame1 = Files.write(tempDir.resolve("f1.jpg"), new byte[]{10});
            Path frame2 = Files.write(tempDir.resolve("f2.jpg"), new byte[]{20});
            FrameBatch batch = new FrameBatch(tempDir, List.of(frame1, frame2));

            when(extractor.extract(videoUrl)).thenReturn(batch);

            ModerationResult cleanResult = ModerationResult.clean();
            ModerationResult flagResult = new ModerationResult(ModerationAction.FLAG_FOR_REVIEW, "harassment", 0.65, null);

            when(imageModerator.moderateImage(eq("image/jpeg"), any(byte[].class)))
                    .thenReturn(cleanResult)
                    .thenReturn(flagResult);

            Optional<ModerationResult> result = stage.evaluate(post);

            assertThat(result).contains(flagResult);
            verify(imageModerator, times(2)).moderateImage(eq("image/jpeg"), any(byte[].class));
        }
    }
}
