package com.elingo.gamification.repository;

import com.elingo.gamification.dto.projection.ActivityByDateProjection;
import com.elingo.gamification.dto.projection.DateCountProjection;
import com.elingo.gamification.entity.XpEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface XpEventRepository extends JpaRepository<XpEvent, Long> {

    interface UserPeriodXp {
        Long getUserId();
        Long getTotalXp();
    }

    @Query("SELECT e.user.id AS userId, SUM(e.amount) AS totalXp " +
            "FROM XpEvent e " +
            "WHERE e.createdAt >= :since " +
            "GROUP BY e.user.id")
    List<UserPeriodXp> sumXpGroupByUserSince(@Param("since") LocalDateTime since);

    @Query(value = "SELECT DATE_FORMAT(e.created_at, '%Y-%m-%d') AS periodKey, COUNT(DISTINCT e.user_id) AS countVal " +
            "FROM xp_events e " +
            "WHERE e.created_at >= :from AND e.created_at < :to AND e.source <> 'ADMIN_ADJUST' " +
            "GROUP BY periodKey ORDER BY periodKey ASC", nativeQuery = true)
    List<DateCountProjection> countDauByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT e.user.id) FROM XpEvent e WHERE e.createdAt >= :from AND e.createdAt < :to AND e.source <> com.elingo.gamification.entity.XpSource.ADMIN_ADJUST")
    long countDistinctUsersBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT
                DATE_FORMAT(e.created_at, '%Y-%m-%d') AS dateKey,
                COUNT(CASE WHEN e.source = 'FLASHCARD_REVIEW' THEN 1 END) AS flashcardCount,
                COUNT(CASE WHEN e.source = 'SEGMENT_DICTATION' THEN 1 END) AS dictationCount,
                COUNT(CASE WHEN e.source = 'SEGMENT_SHADOWING' THEN 1 END) AS shadowingCount,
                COUNT(CASE WHEN e.source IN ('BATTLE_WIN', 'BATTLE_LOSE') THEN 1 END) AS battleCount,
                COUNT(CASE WHEN e.source = 'DAILY_CHECKIN' THEN 1 END) AS checkInCount,
                COUNT(*) AS totalCount
            FROM xp_events e
            WHERE e.created_at >= :from AND e.created_at < :to
            GROUP BY dateKey
            ORDER BY dateKey ASC
            """, nativeQuery = true)
    List<ActivityByDateProjection> countActivityBySourceAndDate(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
