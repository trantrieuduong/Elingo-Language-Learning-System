package com.elingo.battle.dto.request;

import jakarta.validation.constraints.NotNull;

public record RejoinRequest(
        @NotNull(message = "BATTLE_MATCH_NOT_FOUND") 
        Long matchId
) {
}

