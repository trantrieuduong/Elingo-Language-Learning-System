package com.elingo.gamification.repository;

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
}

