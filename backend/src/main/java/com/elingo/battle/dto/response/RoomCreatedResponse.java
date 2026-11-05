package com.elingo.battle.dto.response;

import com.elingo.battle.entity.BattleMode;

public record RoomCreatedResponse(
        String roomCode,
        BattleMode mode
) {
}

