package com.elingo.gamification.repository;

import com.elingo.gamification.entity.LeaderboardPeriodType;
import com.elingo.gamification.entity.LeaderboardSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaderboardSnapshotRepository extends JpaRepository<LeaderboardSnapshot, Long> {

    @Query("""
            SELECT s FROM LeaderboardSnapshot s
            JOIN FETCH s.user
            WHERE s.periodType = :periodType AND s.periodKey = :periodKey
            ORDER BY s.rank ASC
            """)
    Page<LeaderboardSnapshot> findByPeriod(
            @Param("periodType") LeaderboardPeriodType periodType,
            @Param("periodKey") String periodKey,
            Pageable pageable
    );

    boolean existsByPeriodTypeAndPeriodKey(
            LeaderboardPeriodType periodType,
            String periodKey
    );
}
