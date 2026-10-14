package com.elingo.common.event;

import com.elingo.gamification.entity.XpSource;

import java.time.LocalDateTime;

public record XpEvent(
        long userId,
        long refId,
        XpSource xpSource,
        LocalDateTime occurredAt) {
}
