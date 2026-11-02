package com.elingo.gamification.repository;

import com.elingo.gamification.entity.DailyCheckin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCheckInRepository extends JpaRepository<DailyCheckin, Long> {
}
