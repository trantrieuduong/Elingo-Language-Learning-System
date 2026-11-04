package com.elingo.battle.repository;

import com.elingo.battle.entity.BattleMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BattleMatchRepository extends JpaRepository<BattleMatch, Long> {
    boolean existsByRoomCode(String roomCode);

    @Modifying
    @Query("update BattleMatch m set m.status = com.elingo.battle.entity.BattleMatchStatus.CANCELLED, m.finishedAt = :now "
         + "where m.status in (com.elingo.battle.entity.BattleMatchStatus.WAITING, com.elingo.battle.entity.BattleMatchStatus.IN_PROGRESS)")
    int cancelAllUnfinished(@Param("now") LocalDateTime now);
}
