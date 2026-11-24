package com.elingo.gamification.repository;

import com.elingo.gamification.dto.projection.LevelDistribution;
import com.elingo.gamification.entity.UserXp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserXpRepository extends JpaRepository<UserXp, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO user_xp (user_id, total_xp, level_no, last_xp_at, updated_at)
            VALUES (:userId, :amount, 1, NOW(), NOW())
            ON DUPLICATE KEY UPDATE total_xp = total_xp + :amount, last_xp_at = NOW(), updated_at = NOW()
    """, nativeQuery = true)
    void upsertAddXp(@Param("userId") long userId, @Param("amount") int amount);

    @Query("SELECT u.totalXp FROM UserXp u WHERE u.userId = :userId")
    int findTotalXp(@Param("userId") long userId);

    @Modifying
    @Query("""
            UPDATE UserXp u
            SET u.level.levelNo = :level
            WHERE u.userId = :userId AND u.level.levelNo < :level
    """)
    int raiseLevel(@Param("userId") long userId, @Param("level") int level);

    boolean existsByLevelLevelNo(Short levelNo);
           
    @Query("SELECT ux.level.levelNo AS levelNo, ux.level.title AS title, COUNT(ux.userId) AS userCount " +
            "FROM UserXp ux " +
            "GROUP BY ux.level.levelNo, ux.level.title " +
            "ORDER BY ux.level.levelNo ASC")
    List<LevelDistribution> countUsersByLevel();
}
