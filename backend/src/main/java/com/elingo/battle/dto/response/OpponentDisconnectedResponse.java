package com.elingo.battle.dto.response;

public record OpponentDisconnectedResponse(
        Long matchId,
        long graceMs
) {
}

