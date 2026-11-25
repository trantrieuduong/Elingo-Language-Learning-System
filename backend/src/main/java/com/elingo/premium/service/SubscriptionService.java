package com.elingo.premium.service;

import com.elingo.premium.dto.projection.PlanSubscriptionStat;
import com.elingo.premium.entity.UserSubscription;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public interface SubscriptionService {
    UserSubscription activateOrExtend(Long userId, Long planId);
    Duration revokeSubscriptionTime(Long userId, Long subscriptionId, int planDurationDays, LocalDateTime paidAt);
    List<PlanSubscriptionStat> countActiveSubscriptionsByPlan(LocalDateTime now);
    long countDistinctActivePremiumUsers(LocalDateTime now);
    long countTotalActiveSubscriptions(LocalDateTime now);
}
