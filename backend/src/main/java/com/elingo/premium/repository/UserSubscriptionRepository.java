package com.elingo.premium.repository;

import com.elingo.premium.dto.projection.PlanSubscriptionStat;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.entity.UserSubscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    boolean existsByUserIdAndStatusAndEndAtAfter(Long userId, SubscriptionStatus status, LocalDateTime now);

    @EntityGraph(attributePaths = {"plan"})
    Optional<UserSubscription> findFirstByUserIdAndStatusOrderByEndAtDesc(Long userId, SubscriptionStatus status);

    @EntityGraph(attributePaths = {"plan"})
    Optional<UserSubscription> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("UPDATE UserSubscription s SET s.status = :expiredStatus WHERE s.status = :activeStatus AND s.endAt <= :now")
    int updateStatusForExpiredSubscriptions(@Param("expiredStatus") SubscriptionStatus expiredStatus,
                                            @Param("activeStatus") SubscriptionStatus activeStatus,
                                            @Param("now") LocalDateTime now);

    @Query("SELECT s FROM UserSubscription s JOIN FETCH s.user JOIN FETCH s.plan " +
           "WHERE s.status = :status AND s.endAt > :now AND s.endAt <= :threshold")
    List<UserSubscription> findExpiringSubscriptions(@Param("status") SubscriptionStatus status,
                                                     @Param("now") LocalDateTime now,
                                                     @Param("threshold") LocalDateTime threshold);

    @Query("SELECT s.plan.id AS planId, s.plan.name AS planName, COUNT(s.id) AS activeCount " +
            "FROM UserSubscription s " +
            "WHERE s.status = 'ACTIVE' AND s.endAt > :now " +
            "GROUP BY s.plan.id, s.plan.name ORDER BY activeCount DESC")
    List<PlanSubscriptionStat> countActiveSubscriptionsByPlan(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(DISTINCT s.user.id) FROM UserSubscription s WHERE s.status = 'ACTIVE' AND s.endAt > :now")
    long countDistinctActivePremiumUsers(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s.id) FROM UserSubscription s WHERE s.status = 'ACTIVE' AND s.endAt > :now")
    long countTotalActiveSubscriptions(@Param("now") LocalDateTime now);
}
