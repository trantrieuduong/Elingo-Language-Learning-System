package com.elingo.battle.repository;

import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.entity.BattleParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BattleParticipantRepository extends JpaRepository<BattleParticipant, Long> {
    @Query("SELECT p FROM BattleParticipant p JOIN FETCH p.user WHERE p.match.id = :matchId")
    List<BattleParticipant> findByMatchIdWithUser(@Param("matchId") Long matchId);

    Optional<BattleParticipant> findByMatchIdAndUserId(Long matchId, Long userId);

    @Query(value = """
            select p from BattleParticipant p join fetch p.match m
            where p.user.id = :userId
              and m.status = com.elingo.battle.entity.BattleMatchStatus.FINISHED
              and (:filter = 0
                   or (:filter = 1 and p.isWinner = true)
                   or (:filter = 2 and p.isWinner = false and m.winner is not null)
                   or (:filter = 3 and m.winner is null))
              and (:mode is null or m.mode = :mode)
              and (:matchType is null or m.matchType = :matchType)
            order by m.finishedAt desc, m.id desc
            """,
           countQuery = """
            select count(p) from BattleParticipant p join p.match m
            where p.user.id = :userId
              and m.status = com.elingo.battle.entity.BattleMatchStatus.FINISHED
              and (:filter = 0
                   or (:filter = 1 and p.isWinner = true)
                   or (:filter = 2 and p.isWinner = false and m.winner is not null)
                   or (:filter = 3 and m.winner is null))
              and (:mode is null or m.mode = :mode)
              and (:matchType is null or m.matchType = :matchType)
            """)
    Page<BattleParticipant> findHistory(
            @Param("userId") Long userId,
            @Param("filter") int filter,
            @Param("mode") BattleMode mode,
            @Param("matchType") BattleMatchType matchType,
            Pageable pageable
    );

    @Query("select p from BattleParticipant p join fetch p.user where p.match.id in :matchIds")
    List<BattleParticipant> findByMatchIds(@Param("matchIds") Collection<Long> matchIds);

    @Query("""
            select count(p) as total,
                   coalesce(sum(case when p.isWinner = true then 1 else 0 end), 0) as wins,
                   coalesce(sum(case when m.winner is null then 1 else 0 end), 0) as draws
            from BattleParticipant p join p.match m
            where p.user.id = :userId and m.status = com.elingo.battle.entity.BattleMatchStatus.FINISHED
            """)
    BattleStatsProjection getStats(@Param("userId") Long userId);

    interface BattleStatsProjection {
        long getTotal();
        long getWins();
        long getDraws();
    }
}
