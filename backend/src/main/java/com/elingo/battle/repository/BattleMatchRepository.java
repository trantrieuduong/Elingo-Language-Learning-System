package com.elingo.battle.repository;

import com.elingo.battle.entity.BattleMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleMatchRepository extends JpaRepository<BattleMatch, Long> {
}
