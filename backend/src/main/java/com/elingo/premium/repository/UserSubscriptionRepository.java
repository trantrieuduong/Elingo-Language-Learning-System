package com.elingo.premium.repository;

import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    boolean existsByUserIdAndStatusAndEndAtAfter(Long userId, SubscriptionStatus status, LocalDateTime now);
}