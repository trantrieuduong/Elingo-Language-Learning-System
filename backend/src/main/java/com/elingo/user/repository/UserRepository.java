package com.elingo.user.repository;

import com.elingo.user.dto.projection.NewUserStats;
import com.elingo.user.entity.User;
import jakarta.persistence.LockModeType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsById(@NonNull Long id);

    Optional<User> findByGoogleProviderId(String googleProviderId);

    long countByIsVerified(Boolean isVerified);

    long countByIsActive(Boolean isActive);

    long countByCreatedAtBefore(LocalDateTime before);

    @Query(value = "SELECT DATE_FORMAT(u.created_at, '%Y-%m-%d') AS periodKey, COUNT(u.id) AS countVal " +
            "FROM users u " +
            "WHERE u.created_at >= :from AND u.created_at < :to " +
            "GROUP BY periodKey ORDER BY periodKey ASC", nativeQuery = true)
    List<NewUserStats> countNewUsersByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT DATE_FORMAT(DATE_SUB(u.created_at, INTERVAL WEEKDAY(u.created_at) DAY), '%Y-%m-%d') AS periodKey, COUNT(u.id) AS countVal " +
            "FROM users u " +
            "WHERE u.created_at >= :from AND u.created_at < :to " +
            "GROUP BY periodKey ORDER BY periodKey ASC", nativeQuery = true)
    List<NewUserStats> countNewUsersByWeek(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT DATE_FORMAT(u.created_at, '%Y-%m') AS periodKey, COUNT(u.id) AS countVal " +
            "FROM users u " +
            "WHERE u.created_at >= :from AND u.created_at < :to " +
            "GROUP BY periodKey ORDER BY periodKey ASC", nativeQuery = true)
    List<NewUserStats> countNewUsersByMonth(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
