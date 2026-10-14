package com.elingo.gamification.repository;

import com.elingo.gamification.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelRepository extends JpaRepository<Level, Short> {
}
