package com.elingo.gamification.repository;

import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    Optional<Badge> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Badge> findAllByConditionType(BadgeConditionType conditionType);
}
