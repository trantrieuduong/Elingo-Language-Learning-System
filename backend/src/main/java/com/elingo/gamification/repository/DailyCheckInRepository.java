package com.elingo.gamification.repository;

import com.elingo.gamification.entity.DailyCheckin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyCheckInRepository extends JpaRepository<DailyCheckin, Long> {

    @Query("""
            SELECT d.checkinDate FROM DailyCheckin d
            WHERE d.user.id = :userId
              AND d.checkinDate BETWEEN :startDate AND :endDate
            ORDER BY d.checkinDate ASC
            """)
    List<LocalDate> findCheckinDatesBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
