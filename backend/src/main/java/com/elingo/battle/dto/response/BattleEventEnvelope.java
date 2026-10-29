package com.elingo.battle.dto.response;

public record BattleEventEnvelope(
        String event,
        Object data
) {
}
