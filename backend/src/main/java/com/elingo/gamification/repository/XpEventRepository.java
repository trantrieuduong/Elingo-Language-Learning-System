package com.elingo.gamification.repository;

import com.elingo.gamification.entity.XpEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface XpEventRepository extends JpaRepository<XpEvent, Long> {

    interface DateCountProjection {
        String getPeriodKey();
        Long getCountVal();
    }

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
}
