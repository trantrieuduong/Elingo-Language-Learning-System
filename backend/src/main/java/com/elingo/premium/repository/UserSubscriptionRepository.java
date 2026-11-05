package com.elingo.premium.repository;

import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.entity.UserSubscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
}
