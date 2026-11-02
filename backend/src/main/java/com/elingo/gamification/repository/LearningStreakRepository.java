package com.elingo.gamification.repository;

import com.elingo.gamification.entity.LearningStreak;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LearningStreakRepository extends JpaRepository<LearningStreak, Long> {

    /** insert ignore: chưa có -> tạo dòng mới, đã có -> không làm gì, không raise lỗi */
    @Modifying
    @Query(value = "INSERT IGNORE INTO learning_streaks(user_id, current_streak, longest_streak, updated_at) VALUES (:userId, 0, 0, NOW())",
            nativeQuery = true)
    void insertIgnore(@Param("userId") long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM LearningStreak s WHERE s.userId = :userId")
    Optional<LearningStreak> findByIdForUpdate(@Param("userId") long userId);
}
