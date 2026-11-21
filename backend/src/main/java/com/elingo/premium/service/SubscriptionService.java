package com.elingo.premium.service;

import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.repository.UserSubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SubscriptionService {
    UserSubscription activateOrExtend(Long userId, Long planId);
    List<UserSubscriptionRepository.PlanSubscriptionStat> countActiveSubscriptionsByPlan(LocalDateTime now);
    long countDistinctActivePremiumUsers(LocalDateTime now);
    long countTotalActiveSubscriptions(LocalDateTime now);
}
