package com.elingo.battle.repository;

import com.elingo.battle.entity.BattleQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleQuestionRepository extends JpaRepository<BattleQuestion, Long> {
}
