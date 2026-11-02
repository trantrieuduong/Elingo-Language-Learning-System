package com.elingo.battle.repository;

import com.elingo.battle.entity.BattleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BattleParticipantRepository extends JpaRepository<BattleParticipant, Long> {
    @Query("SELECT p FROM BattleParticipant p JOIN FETCH p.user WHERE p.match.id = :matchId")
    List<BattleParticipant> findByMatchIdWithUser(@Param("matchId") Long matchId);

    Optional<BattleParticipant> findByMatchIdAndUserId(Long matchId, Long userId);

    @Query("select count(p) from BattleParticipant p where p.user.id = :userId and p.isWinner = true and p.match.status = com.elingo.battle.entity.BattleMatchStatus.FINISHED")
    long countWins(@Param("userId") Long userId);
}
