package com.elingo.battle.dto.response;

import java.util.List;

public record MatchFinishedResponse(
        Long matchId,
        Long winnerId,
        String reason,
        List<PlayerFinalResultResponse> players
) {
}

