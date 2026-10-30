package com.elingo.battle.service;

import com.elingo.battle.dto.request.RoundAnswerRecord;
import com.elingo.battle.dto.response.CreatedMatchResult;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMode;

import java.util.List;
import java.util.Map;

public interface BattleMatchPersistenceService {
    /**
     * Lưu BattleMatch + 2 BattleParticipant + câu hỏi trong một transaction.
     * Trả về dữ liệu cần dùng sau commit (không gửi event bên trong).
     */
    CreatedMatchResult persistNewRandomMatch(Long userAId, Long userBId, BattleMode mode);

    void updateMatchStatus(Long matchId, BattleMatchStatus status);
    void saveRoundAnswers(List<RoundAnswerRecord> records);
    void finishMatch(Long matchId, Long winnerUserId,
                     Map<Long, Integer> scores, Map<Long, Integer> correctCounts);
}
