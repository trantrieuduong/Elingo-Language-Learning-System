package com.elingo.common.event;

import java.time.LocalDateTime;

public record LevelUpEvent(long userId, int newLevel, LocalDateTime occurredAt) {
}
