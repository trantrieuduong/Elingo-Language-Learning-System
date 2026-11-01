package com.elingo.battle.state;

import com.elingo.battle.entity.BattleMode;

public record LiveRoom(
        String roomCode,
        Long hostId,
        BattleMode mode
) {
}

