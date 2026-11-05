package com.elingo.battle.dto.response;

import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.entity.BattleResult;

import java.time.LocalDateTime;

public record BattleHistoryItemResponse(
        Long matchId,
        BattleMode mode,
        BattleMatchType matchType,
        PlayerResponse opponent,
        int myScore,
        int opponentScore,
        BattleResult result,
        LocalDateTime finishedAt
) {
}
