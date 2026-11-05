package com.elingo.battle.dto.response;

import java.util.List;

public record BattleRoundDetailResponse(
        int roundNo,
        String term,
        String correctAnswer,
        List<BattleRoundAnswerResponse> answers
) {
}

