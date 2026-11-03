package com.elingo.battle.dto.response;

public record MatchCancelledResponse(
        Long matchId,
        String reason
) {
}

