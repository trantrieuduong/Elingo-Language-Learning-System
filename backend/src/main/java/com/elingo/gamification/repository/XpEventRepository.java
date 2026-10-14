package com.elingo.gamification.repository;

import com.elingo.gamification.entity.XpEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XpEventRepository extends JpaRepository<XpEvent, Long> {
}
