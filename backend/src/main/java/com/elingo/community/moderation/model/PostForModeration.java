package com.elingo.community.moderation.model;

import com.elingo.common.enums.MediaKind;

import java.util.List;

public record PostForModeration(
        long id,
        long userId,
        String title,
        String content,
        List<PostMediaItem> medias) {
    public record PostMediaItem(MediaKind mediaKind, String mediaUrl) {}
}
