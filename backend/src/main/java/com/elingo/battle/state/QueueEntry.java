package com.elingo.battle.state;

import com.elingo.battle.entity.BattleMode;

import java.time.Instant;

/**
 * POJO lưu trong RAM, đại diện một entry trong hàng đợi tìm trận.
 */
public record QueueEntry(
        Long userId,
        BattleMode mode,
        Instant joinedAt
) {
}
