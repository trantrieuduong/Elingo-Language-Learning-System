package com.elingo.community.moderation.moderator;

import com.elingo.community.moderation.model.ModerationResult;

public interface TextModerator {
    boolean supports(String text);
    ModerationResult moderateText(String text);
}
