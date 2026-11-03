package com.elingo.battle.dto.request;

import com.elingo.battle.entity.BattleMode;
import jakarta.validation.constraints.NotNull;

public record RoomCreateRequest(
        @NotNull(message = "BATTLE_MODE_INVALID") 
        BattleMode mode
) {
}

