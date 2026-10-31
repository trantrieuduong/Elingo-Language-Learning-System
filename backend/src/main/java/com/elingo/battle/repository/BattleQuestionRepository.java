package com.elingo.battle.repository;

import com.elingo.battle.entity.BattleQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BattleQuestionRepository extends JpaRepository<BattleQuestion, Long> {
    @Query("""
            select c.id from Card c
            join c.deck d
            where d.status = com.elingo.vocabulary.entity.DeckStatus.PUBLISHED
              and d.ownerType = com.elingo.vocabulary.entity.OwnerType.SYSTEM
              and c.term is not null and trim(c.term) <> ''
              and c.translation is not null and trim(c.translation) <> ''
            """)
    List<Long> findBattleEligibleCardIds();

    @Query("""
            select c.id from Card c
            join c.deck d
            where d.status = com.elingo.vocabulary.entity.DeckStatus.PUBLISHED
              and d.ownerType = com.elingo.vocabulary.entity.OwnerType.SYSTEM
              and c.term is not null and trim(c.term) <> ''
              and c.translation is not null and trim(c.translation) <> ''
              and length(c.term) <= 40
              and c.term not like '%/%' and c.term not like '%;%' and c.term not like '%,%'
            """)
    List<Long> findBattleTypingEligibleCardIds();
}
