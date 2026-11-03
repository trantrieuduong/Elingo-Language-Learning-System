package com.elingo.battle.repository;

import com.elingo.battle.entity.BattleAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BattleAnswerRepository extends JpaRepository<BattleAnswer, Long> {
    @Query("""
            select a from BattleAnswer a
            join fetch a.participant p join fetch p.user
            left join fetch a.selectedOption
            where a.question.match.id = :matchId
            """)
    List<BattleAnswer> findByMatchIdWithDetails(@Param("matchId") Long matchId);
}
