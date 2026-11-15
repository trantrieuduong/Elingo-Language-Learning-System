package com.elingo.common.event;

import com.elingo.community.entity.ModerationAction;

public record PostModeratedEvent(long postId, long userId, ModerationAction action) {
}
