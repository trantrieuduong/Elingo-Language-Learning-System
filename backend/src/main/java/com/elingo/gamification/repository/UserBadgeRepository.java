package com.elingo.gamification.repository;

import com.elingo.gamification.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    @Query("""
            SELECT ub FROM UserBadge ub
            JOIN FETCH ub.badge
            WHERE ub.user.id = :userId
            ORDER BY ub.earnedAt DESC
            """)
    List<UserBadge> findAllByUserIdWithBadge(@Param("userId") Long userId);

    @Query("SELECT ub.badge.id FROM UserBadge ub WHERE ub.user.id = :userId")
    Set<Long> findEarnedBadgeIdsByUserId(@Param("userId") Long userId);

    boolean existsByBadgeId(Long badgeId);
}
