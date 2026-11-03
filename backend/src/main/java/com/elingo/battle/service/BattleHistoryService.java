package com.elingo.battle.service;

import com.elingo.battle.dto.response.BattleHistoryItemResponse;
import com.elingo.battle.dto.response.BattleMatchDetailResponse;
import com.elingo.battle.dto.response.BattleStatsResponse;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.entity.BattleResult;
import com.elingo.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface BattleHistoryService {
    PageResponse<BattleHistoryItemResponse> getHistory(
            Long userId,
            BattleResult result,
            BattleMode mode,
            BattleMatchType matchType,
            Pageable pageable
    );
    BattleMatchDetailResponse getMatchDetail(Long userId, Long matchId);
    BattleStatsResponse getStats(Long userId);
}
