package com.elingo.community.moderation.stage.impl;

import com.elingo.common.enums.MediaKind;
import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.service.FfmpegFrameExtractor;
import com.elingo.community.moderation.model.FrameBatch;
import com.elingo.community.moderation.model.*;
import com.elingo.community.moderation.moderator.ImageModerator;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.elingo.community.moderation.stage.ModerationStage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class VideoStage implements ModerationStage {

    private final FfmpegFrameExtractor extractor;
    private final ImageModerator imageModerator;

    public VideoStage(FfmpegFrameExtractor extractor, ImageModerator imageModerator) {
        this.extractor = extractor;
        this.imageModerator = imageModerator;
    }

    @Override
    public String name() {
        return "VIDEO";
    }

    @Override
    public Optional<ModerationResult> evaluate(PostForModeration post) {
        List<PostForModeration.PostMediaItem> videos = post.medias().stream()
                .filter(postMediaItem -> postMediaItem.mediaKind().equals(MediaKind.VIDEO))
                .toList();
        if (videos.isEmpty())
            return Optional.empty();

        ModerationResult worst = ModerationResult.clean();
        for (PostForModeration.PostMediaItem video : videos) {
            try (FrameBatch frames = extractor.extract(video.mediaUrl())) {
                for (Path file : frames.getFrames()) {
                    ModerationResult result = imageModerator.moderateImage("image/jpeg", FrameBatch.read(file));
                    if (result.action().equals(ModerationAction.AUTO_REJECT))
                        return Optional.of(result);
                    worst = worst.worseOf(result);
                }
            }
        }
        return Optional.of(worst);
    }
}
