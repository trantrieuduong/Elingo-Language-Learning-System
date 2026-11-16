package com.elingo.gamification.repository;

import com.elingo.gamification.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, Long> { }
