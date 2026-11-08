package com.elingo.community.moderation.moderator;

import com.elingo.community.moderation.model.ModerationResult;

public interface ImageModerator {
    ModerationResult moderateImage(String publicImageUrl);
    ModerationResult moderateImage(String mimeType, byte[] bytes);
}
