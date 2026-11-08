package com.elingo.community.moderation.stage.impl;

import com.elingo.common.enums.MediaKind;
import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.model.*;
import com.elingo.community.moderation.moderator.ImageModerator;

import java.util.List;
import java.util.Optional;

import com.elingo.community.moderation.stage.ModerationStage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class ImageStage implements ModerationStage {
    private final ImageModerator imageModerator;

    public ImageStage(ImageModerator imageModerator) {
        this.imageModerator = imageModerator;
    }

    @Override
    public String name() {
        return "IMAGE";
    }

    @Override
    public Optional<ModerationResult> evaluate(PostForModeration post) {
        List<PostForModeration.PostMediaItem> images = post.medias().stream()
                .filter(postMediaItem -> postMediaItem.mediaKind().equals(MediaKind.IMAGE))
                .toList();
        if (images.isEmpty())
            return Optional.empty();

        ModerationResult worst = ModerationResult.clean();
        for (PostForModeration.PostMediaItem image : images) {
            ModerationResult result = imageModerator.moderateImage(image.mediaUrl());
            if (result.action().equals(ModerationAction.AUTO_REJECT))
                return Optional.of(result);
            worst = worst.worseOf(result);
        }

        return Optional.of(worst);
    }
}
