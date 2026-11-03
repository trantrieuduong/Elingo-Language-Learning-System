package com.elingo.battle.dto.response;

import java.util.List;

public record RoundResultResponse(
        Long matchId,
        int roundNo,
        String correctAnswer,
        List<PlayerRoundResultResponse> results
) {
}

